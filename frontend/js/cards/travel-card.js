import { api } from '../api.js';
import { cardError, h, numberInput, segmented, select, skeleton, uid } from '../dom.js';
import { formatNumber } from '../format.js';
import { bindCalculator, readNumber } from '../components/calculator.js';
import { createResultPanel } from '../components/result-panel.js';
import { unitOption } from './unit-card.js';

/**
 * A value + unit pair. When it is the quantity being solved for, only the unit picker remains
 * (it chooses the unit of the result).
 */
function quantityGroup({ label, noun, units, defaultUnit, placeholder }) {
  const input = numberInput({ placeholder });
  input.id = uid('qty');
  const unitSelect = select(units.map(unitOption), defaultUnit);
  unitSelect.id = uid('unit');
  unitSelect.setAttribute('aria-label', `${label} unit`);
  const labelEl = h('label', { class: 'field-label', for: input.id }, label);
  const row = h('div', { class: 'input-group' }, input, unitSelect);
  const el = h('div', { class: 'field' }, labelEl, row);

  return {
    el,
    input,
    unitSelect,
    noun,
    setSolved(solved) {
      input.hidden = solved;
      row.classList.toggle('solo', solved);
      labelEl.textContent = solved ? `${label} — show result in` : label;
      labelEl.htmlFor = solved ? unitSelect.id : input.id;
      unitSelect.setAttribute('aria-label', solved ? `Result unit for ${noun}` : `${label} unit`);
    }
  };
}

export async function mountTravelCard(container) {
  container.replaceChildren(skeleton(4));
  let options;
  try {
    options = await api.travelOptions();
  } catch (error) {
    container.replaceChildren(cardError(error.message, () => mountTravelCard(container)));
    return;
  }

  const groups = {
    DISTANCE: quantityGroup({ label: 'Distance', noun: 'distance', units: options.distanceUnits, defaultUnit: options.defaultDistanceUnit, placeholder: 'e.g. 500' }),
    SPEED: quantityGroup({ label: 'Speed', noun: 'speed', units: options.speedUnits, defaultUnit: options.defaultSpeedUnit, placeholder: 'e.g. 80' }),
    TIME: quantityGroup({ label: 'Time', noun: 'time', units: options.timeUnits, defaultUnit: options.defaultTimeUnit, placeholder: 'e.g. 3' })
  };

  const mode = segmented('solve', [
    { value: 'TIME', label: 'Time' },
    { value: 'DISTANCE', label: 'Distance' },
    { value: 'SPEED', label: 'Speed' }
  ], 'TIME', 'Calculate');

  const EMPTY_TEXT = 'Fill in two values, then calculate.';
  const panel = createResultPanel({ placeholder: EMPTY_TEXT });
  const submit = h('button', { type: 'submit', class: 'btn btn-primary' }, 'Calculate time');

  const applyMode = () => {
    for (const [key, group] of Object.entries(groups)) group.setSolved(key === mode.value);
    submit.textContent = `Calculate ${groups[mode.value].noun}`;
  };
  mode.onChange(() => {
    applyMode();
    panel.showEmpty(EMPTY_TEXT);
  });
  applyMode();

  const form = h('form', { class: 'form', novalidate: true },
    h('div', { class: 'field' },
      h('span', { class: 'field-label', 'aria-hidden': 'true' }, 'Calculate'),
      mode.el),
    groups.DISTANCE.el,
    groups.SPEED.el,
    groups.TIME.el,
    submit);

  bindCalculator({
    form,
    panel,
    buildRequest: () => {
      const solveFor = mode.value;
      const request = { solveFor };
      for (const [key, group] of Object.entries(groups)) {
        const field = key.toLowerCase();
        request[`${field}Unit`] = group.unitSelect.value;
        if (key !== solveFor) request[field] = readNumber(group.input, `the ${group.noun}`);
      }
      return request;
    },
    execute: api.calculateTravel,
    render: (response) => {
      const known = response.inputs.map((q) => `${formatNumber(q.value)} ${q.unit.symbol}`);
      const basis = response.solveFor === 'DISTANCE'
        ? `${known[0]} for ${known[1]}`
        : response.solveFor === 'SPEED' ? `${known[0]} in ${known[1]}` : `${known[0]} at ${known[1]}`;
      const value = formatNumber(response.result.value);

      if (response.solveFor === 'TIME') {
        panel.showResult({
          from: basis,
          value: response.readable,
          caption: `= ${value} ${response.result.unit.name.toLowerCase()}${value === '1' ? '' : 's'}`,
          detail: response.formula,
          copy: response.readable
        });
      } else {
        panel.showResult({
          from: basis,
          value,
          unit: response.result.unit.symbol,
          caption: response.result.unit.name,
          detail: response.formula
        });
      }
    }
  });

  container.replaceChildren(form, panel.el);
}
