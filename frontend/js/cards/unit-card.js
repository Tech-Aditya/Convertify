import { api } from '../api.js';
import { cardError, field, h, numberInput, select, setOptions, skeleton, swapButton } from '../dom.js';
import { formatNumber, parseDecimal } from '../format.js';
import { bindCalculator, readNumber } from '../components/calculator.js';
import { createResultPanel } from '../components/result-panel.js';
import { createAllUnitsDisclosure } from '../components/all-units.js';

/** "Kilometers per hour (km/h)"; the symbol is omitted when it just repeats the name. */
export const unitOption = (unit) => ({
  value: unit.code,
  label: unit.symbol.toLowerCase() === unit.name.toLowerCase() ? unit.name : `${unit.name} (${unit.symbol})`
});

/**
 * Generic unit converter card. Driven entirely by the backend catalog:
 * - data-preset="height" shows a curated preset (Height, Distance, Speed);
 * - data-category-picker="true" lets the user choose any category.
 */
export async function mountUnitCard(container) {
  const presetCode = container.dataset.preset;
  const withCategoryPicker = container.dataset.categoryPicker === 'true';

  container.replaceChildren(skeleton(3));
  let catalog;
  try {
    catalog = await api.unitCatalog();
  } catch (error) {
    container.replaceChildren(cardError(error.message, () => mountUnitCard(container)));
    return;
  }

  const preset = presetCode ? catalog.presets.find((p) => p.code === presetCode) : null;
  if (presetCode && !preset) {
    container.replaceChildren(cardError('This converter is not available right now.', () => mountUnitCard(container)));
    return;
  }

  // Current view: the preset, or the chosen category (preset categories first for familiarity).
  const views = preset
    ? [{ category: preset.category, units: preset.units, from: preset.defaultFrom, to: preset.defaultTo }]
    : catalog.categories.map((c) => ({ category: c.code, name: c.name, units: c.units, from: c.units[0].code, to: c.units[1]?.code ?? c.units[0].code }));
  let view = withCategoryPicker ? views.find((v) => v.category === 'WEIGHT') ?? views[0] : views[0];

  const categorySelect = withCategoryPicker
    ? select(views.map((v) => ({ value: v.category, label: v.name })), view.category)
    : null;
  const examplePlaceholder = container.dataset.placeholder ?? 'e.g. 1';
  const valueInput = numberInput({ placeholder: examplePlaceholder });
  const fromSelect = select([], view.from);
  const toSelect = select([], view.to);
  const EMPTY_TEXT = 'Enter a value, then convert.';
  const panel = createResultPanel({ placeholder: EMPTY_TEXT });

  const unitsOf = () => view.units;

  function applyView(next) {
    view = next;
    setOptions(fromSelect, unitsOf().map(unitOption), view.from);
    setOptions(toSelect, unitsOf().map(unitOption), view.to);
    valueInput.placeholder = view.category === 'TEMPERATURE' ? 'e.g. 36.6' : examplePlaceholder;
  }
  applyView(view);

  const allUnits = createAllUnitsDisclosure({
    load: async () => {
      const value = parseDecimal(valueInput.value);
      if (value === null) return null;
      const response = await api.convertAllUnits({
        category: view.category,
        value,
        from: fromSelect.value,
        units: preset ? preset.units.map((u) => u.code) : undefined
      });
      return response.results;
    }
  });

  const form = h('form', { class: 'form', novalidate: true },
    categorySelect
      ? h('div', { class: 'form-row' }, field('Category', categorySelect).el, field('Value', valueInput).el)
      : field('Value', valueInput).el,
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
      category: view.category,
      value: readNumber(valueInput, 'a value'),
      from: fromSelect.value,
      to: toSelect.value
    }),
    execute: api.convertUnits,
    render: (response) => {
      const { input, result, unitRate } = response;
      const value = formatNumber(result.value);
      panel.showResult({
        from: `${formatNumber(input.value)} ${input.unit.symbol}`,
        value,
        unit: result.unit.symbol,
        caption: `${input.unit.name} → ${result.unit.name}`,
        detail: input.value !== '1' ? `1 ${input.unit.symbol} = ${formatNumber(unitRate)} ${result.unit.symbol}` : null,
        copy: `${value} ${result.unit.symbol}`
      });
      allUnits.refresh();
    }
  });

  categorySelect?.addEventListener('change', () => {
    applyView(views.find((v) => v.category === categorySelect.value));
    panel.showEmpty(EMPTY_TEXT);
    allUnits.refresh();
  });
  fromSelect.addEventListener('change', () => allUnits.refresh());

  container.replaceChildren(form, panel.el, allUnits.el);
}
