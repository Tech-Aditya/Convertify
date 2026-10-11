import { h } from '../dom.js';
import { formatNumber } from '../format.js';

/**
 * Progressive disclosure listing one value in many units. Loads only when opened, and
 * reloads after each new result while open.
 *
 * @param load async () => [{ value, unit: { symbol, name } }] or null when there is no input yet
 */
export function createAllUnitsDisclosure({ summary = 'Show in all units', load }) {
  const list = h('ul', { class: 'all-units' });
  const status = h('p', { class: 'all-units-status' });
  const el = h('details', { class: 'disclosure' },
    h('summary', {}, summary),
    h('div', { class: 'disclosure-body' }, status, list));

  let sequence = 0;

  async function refresh() {
    if (!el.open) return;
    const current = ++sequence;
    status.textContent = 'Loading…';
    status.hidden = false;
    try {
      const results = await load();
      if (current !== sequence) return;
      if (!results) {
        status.textContent = 'Enter a value above to see it in every unit.';
        list.replaceChildren();
        return;
      }
      status.hidden = true;
      list.replaceChildren(...results.map(({ value, unit }) => h('li', {},
        h('span', { class: 'unit' }, unit.name),
        h('span', { class: 'qty' }, `${formatNumber(value)} ${unit.symbol}`))));
    } catch (error) {
      if (current !== sequence) return;
      status.textContent = error.message;
      list.replaceChildren();
    }
  }

  el.addEventListener('toggle', refresh);
  return { el, refresh };
}
