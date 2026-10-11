import { api } from '../api.js';
import { cardError, field, h, numberInput, select, setOptions, skeleton, swapButton } from '../dom.js';
import { formatNumber, parseDecimal } from '../format.js';
import { bindCalculator, readNumber } from '../components/calculator.js';
import { createResultPanel } from '../components/result-panel.js';
import { createAllUnitsDisclosure } from '../components/all-units.js';
import { unitOption } from './unit-card.js';

const DEFAULT_FROM = 'ac';
const FALLBACK_TO = 'ha';

/** Standard units first, then the region's traditional units (which carry a conversion basis). */
function groupedUnits(region) {
  const standard = region.units.filter((u) => !u.basis);
  const regional = region.units.filter((u) => u.basis);
  return regional.length
    ? [{ label: 'Standard units', options: standard.map(unitOption) },
       { label: `${region.name} units`, options: regional.map(unitOption) }]
    : standard.map(unitOption);
}

function basisText(region) {
  const regional = region.units.filter((u) => u.basis);
  return regional.length
    ? regional.map((u) => u.basis).join(' · ')
    : 'This region uses standard metric and imperial units only.';
}

export async function mountLandCard(container) {
  container.replaceChildren(skeleton(4));
  let data;
  try {
    data = await api.landRegions();
  } catch (error) {
    container.replaceChildren(cardError(error.message, () => mountLandCard(container)));
    return;
  }

  const regionByCode = new Map(data.regions.map((r) => [r.code, r]));
  const regionSelect = select(data.regions.map((r) => ({ value: r.code, label: r.name })), data.defaultRegion);
  const valueInput = numberInput({ placeholder: 'e.g. 1' });
  const fromSelect = select([]);
  const toSelect = select([]);
  const EMPTY_TEXT = 'Enter an area, then convert.';
  const panel = createResultPanel({ placeholder: EMPTY_TEXT });

  const region = () => regionByCode.get(regionSelect.value);

  function applyRegion() {
    const current = region();
    const codes = current.units.map((u) => u.code);
    const firstRegional = current.units.find((u) => u.basis)?.code;
    const keep = (code, fallback) => (codes.includes(code) ? code : fallback);
    setOptions(fromSelect, groupedUnits(current), keep(fromSelect.value || DEFAULT_FROM, DEFAULT_FROM));
    setOptions(toSelect, groupedUnits(current), keep(toSelect.value || firstRegional || FALLBACK_TO, firstRegional ?? FALLBACK_TO));
    // The region's conversion basis is shown in the empty result, before the first conversion.
    if (panel.el.dataset.state === 'empty') panel.showEmpty(EMPTY_TEXT, basisText(current));
  }
  applyRegion();

  const allUnits = createAllUnitsDisclosure({
    load: async () => {
      const value = parseDecimal(valueInput.value);
      if (value === null) return null;
      const response = await api.convertAllLand({ region: regionSelect.value, value, from: fromSelect.value });
      return response.results;
    }
  });

  const form = h('form', { class: 'form', novalidate: true },
    h('div', { class: 'form-row' }, field('Region', regionSelect).el, field('Area', valueInput).el),
    h('div', { class: 'form-row units' },
      field('From', fromSelect).el,
      swapButton('Swap units', () => {
        [fromSelect.value, toSelect.value] = [toSelect.value, fromSelect.value];
        calculator.refresh();
        allUnits.refresh();
      }),
      field('To', toSelect).el),
    h('button', { type: 'submit', class: 'btn btn-primary' }, 'Convert'));

  const calculator = bindCalculator({
    form,
    panel,
    buildRequest: () => ({
      region: regionSelect.value,
      value: readNumber(valueInput, 'an area'),
      from: fromSelect.value,
      to: toSelect.value
    }),
    execute: api.convertLand,
    render: (response) => {
      const { input, result } = response;
      const value = formatNumber(result.value);
      panel.showResult({
        from: `${formatNumber(input.value)} ${input.unit.symbol}`,
        value,
        unit: result.unit.symbol,
        caption: `${input.unit.name} → ${result.unit.name} · ${response.region.name}`,
        detail: [
          input.value !== '1' ? `1 ${input.unit.symbol} = ${formatNumber(response.unitRate)} ${result.unit.symbol}` : null,
          // Conversion basis of the regional units involved; the region is already in the caption.
          ...(response.notes ?? []).map((n) => n.replace(/\s*\([^)]*\)$/, ''))
        ],
        // Traditional units vary locally, so a one-line caveat travels with every result (full text on hover).
        meta: response.disclaimer
          ? h('span', { class: 'result-caveat', title: response.disclaimer }, 'Local definitions can vary: verify with land records.')
          : null,
        copy: `${value} ${result.unit.symbol}`
      });
      allUnits.refresh();
    }
  });

  regionSelect.addEventListener('change', () => {
    applyRegion();
    allUnits.refresh();
  });
  fromSelect.addEventListener('change', () => allUnits.refresh());

  regionSelect.title = data.disclaimer;
  container.replaceChildren(form, panel.el, allUnits.el);
}
