import { api } from '../api.js';
import { cardError, field, h, numberInput, skeleton, swapButton } from '../dom.js';
import { formatNumber, parseDecimal } from '../format.js';
import { bindCalculator, readNumber } from '../components/calculator.js';
import { createCombobox } from '../components/combobox.js';
import { createResultPanel } from '../components/result-panel.js';
import { createAllUnitsDisclosure } from '../components/all-units.js';
import { UNIT_ALIASES } from './unit-aliases.js';

/** "Kilometers per hour (km/h)"; the symbol is omitted when it just repeats the name. */
export const unitOption = (unit) => ({
  value: unit.code,
  label: unit.symbol.toLowerCase() === unit.name.toLowerCase() ? unit.name : `${unit.name} (${unit.symbol})`
});

/** Searchable-picker item for a unit: found by name, symbol, code or a common alias. */
const unitItem = (unit) => ({
  ...unitOption(unit), code: unit.code, name: unit.name, symbol: unit.symbol, keywords: UNIT_ALIASES[unit.code]
});

const CATEGORY_ALIASES = {
  LENGTH: 'distance height width depth', AREA: 'land size surface', SPEED: 'velocity pace',
  TIME: 'duration', WEIGHT: 'mass', TEMPERATURE: 'heat degrees', VOLUME: 'capacity liquid'
};

const unitPicker = (units, value) => createCombobox({
  items: units.map(unitItem), value, layout: 'name', noun: 'units', searchHint: 'Search units',
  emptyHint: 'Try a name (feet), a symbol (km/h) or an abbreviation (lbs).'
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

  const categoryPicker = withCategoryPicker
    ? createCombobox({
      items: views.map((v) => ({
        value: v.category, code: v.category, name: v.name, label: v.name, keywords: CATEGORY_ALIASES[v.category]
      })),
      value: view.category, layout: 'name', noun: 'categories', searchHint: 'Search categories'
    })
    : null;
  const examplePlaceholder = container.dataset.placeholder ?? 'e.g. 1';
  const valueInput = numberInput({ placeholder: examplePlaceholder });
  const fromPicker = unitPicker(view.units, view.from);
  const toPicker = unitPicker(view.units, view.to);
  const EMPTY_TEXT = 'Enter a value, then convert.';
  const panel = createResultPanel({ placeholder: EMPTY_TEXT });

  function applyView(next) {
    view = next;
    fromPicker.setItems(view.units.map(unitItem), view.from);
    toPicker.setItems(view.units.map(unitItem), view.to);
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
        from: fromPicker.value,
        units: preset ? preset.units.map((u) => u.code) : undefined
      });
      return response.results;
    }
  });

  const form = h('form', { class: 'form', novalidate: true },
    categoryPicker
      ? h('div', { class: 'form-row' },
        field('Category', categoryPicker.el, { labelFor: categoryPicker.input }).el, field('Value', valueInput).el)
      : field('Value', valueInput).el,
    h('div', { class: 'form-row units' },
      field('From', fromPicker.el, { labelFor: fromPicker.input }).el,
      swapButton('Swap units', () => {
        [fromPicker.value, toPicker.value] = [toPicker.value, fromPicker.value];
        calculator.refresh();
        allUnits.refresh();
      }),
      field('To', toPicker.el, { labelFor: toPicker.input }).el),
    h('button', { type: 'submit', class: 'btn btn-primary' }, 'Convert'));

  const calculator = bindCalculator({
    form,
    panel,
    buildRequest: () => ({
      category: view.category,
      value: readNumber(valueInput, 'a value'),
      from: fromPicker.value,
      to: toPicker.value
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

  categoryPicker?.onChange((category) => {
    applyView(views.find((v) => v.category === category));
    panel.showEmpty(EMPTY_TEXT);
    allUnits.refresh();
  });
  fromPicker.onChange(() => allUnits.refresh());

  container.replaceChildren(form, panel.el, allUnits.el);
}
