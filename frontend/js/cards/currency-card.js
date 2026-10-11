import { api } from '../api.js';
import { cardError, field, h, numberInput, skeleton, swapButton } from '../dom.js';
import { formatCurrency, formatDateTime, formatNumber } from '../format.js';
import { bindCalculator, readNumber } from '../components/calculator.js';
import { createCombobox } from '../components/combobox.js';
import { createResultPanel } from '../components/result-panel.js';
import { CURRENCY_ALIASES } from './currency-aliases.js';

/** "₩" for KRW; null when the browser has no symbol of its own for the code. */
function currencySymbol(code) {
  try {
    const symbol = new Intl.NumberFormat('en', { style: 'currency', currency: code, currencyDisplay: 'narrowSymbol' })
      .formatToParts(0).find((part) => part.type === 'currency')?.value;
    return symbol && symbol !== code ? symbol : null;
  } catch {
    return null;
  }
}

/** Combobox items; the API lists featured currencies first, in their configured order. */
const currencyItems = (currencies) => {
  const grouped = currencies.some((c) => c.featured);
  return currencies.map((c) => ({
    value: c.code,
    code: c.code,
    name: c.name,
    symbol: currencySymbol(c.code),
    keywords: CURRENCY_ALIASES[c.code],
    group: grouped ? (c.featured ? 'Popular' : 'All currencies') : undefined
  }));
};

const currencyPicker = (items, value) =>
  createCombobox({ items, value, noun: 'currencies', searchHint: 'Search currency or country' });

/** One-line provenance for the card footer, e.g. "Rates updated Sep 29, 5:32 AM · ExchangeRate-API". */
function provenanceText(rates) {
  return [rates.lastUpdated ? `Rates updated ${formatDateTime(rates.lastUpdated)}` : null, rates.source]
    .filter(Boolean).join(' · ');
}

/** Shown in the result only when the provider was unreachable and older rates were used. */
function staleWarning(rates) {
  return rates.stale
    ? [h('span', { class: 'badge-warning' },
        h('span', { 'aria-hidden': 'true' }, '!'),
        'Live rates unavailable — showing the most recent saved rates')]
    : [];
}

export async function mountCurrencyCard(container) {
  container.replaceChildren(skeleton(3));
  let data;
  try {
    data = await api.currencies(true);
  } catch (error) {
    container.replaceChildren(cardError(error.message, () => mountCurrencyCard(container)));
    return;
  }

  const byCode = new Map(data.currencies.map((c) => [c.code, c]));
  const amountInput = numberInput({ placeholder: 'e.g. 100' });
  const items = currencyItems(data.currencies);
  const fromPicker = currencyPicker(items, data.defaultFrom);
  const toPicker = currencyPicker(items, data.defaultTo);
  const panel = createResultPanel({ placeholder: 'Enter an amount, then convert.' });
  const footnote = h('p', { class: 'card-foot', title: provenanceText(data.rates) }, provenanceText(data.rates));

  const form = h('form', { class: 'form', novalidate: true },
    field('Amount', amountInput).el,
    h('div', { class: 'form-row units' },
      field('From', fromPicker.el, { labelFor: fromPicker.input }).el,
      swapButton('Swap currencies', () => {
        [fromPicker.value, toPicker.value] = [toPicker.value, fromPicker.value];
        calculator.refresh();
      }),
      field('To', toPicker.el, { labelFor: toPicker.input }).el),
    h('button', { type: 'submit', class: 'btn btn-primary' }, 'Convert'));

  const calculator = bindCalculator({
    form,
    panel,
    buildRequest: () => ({ amount: readNumber(amountInput, 'an amount'), from: fromPicker.value, to: toPicker.value }),
    execute: api.convertCurrency,
    render: (response) => {
      const { from, to } = response;
      const amountText = formatCurrency(response.amount, from.code, byCode.get(from.code)?.fractionDigits ?? 2);
      const resultText = formatCurrency(response.result, to.code, to.fractionDigits);
      panel.showResult({
        from: amountText,
        value: resultText,
        caption: `${from.name} → ${to.name}`,
        detail: [
          `1 ${from.code} = ${formatNumber(response.rate)} ${to.code}`,
          `1 ${to.code} = ${formatNumber(response.inverseRate)} ${from.code}`
        ],
        meta: staleWarning(response.rates),
        copy: `${resultText} ${to.code}`
      });
      footnote.textContent = footnote.title = provenanceText(response.rates);
    }
  });

  container.replaceChildren(form, panel.el, footnote);
}
