import { api } from '../api.js';
import { cardError, field, h, numberInput, select, skeleton, swapButton } from '../dom.js';
import { formatCurrency, formatDateTime, formatNumber } from '../format.js';
import { bindCalculator, readNumber } from '../components/calculator.js';
import { createResultPanel } from '../components/result-panel.js';

const currencyOptions = (currencies) => {
  const option = (c) => ({ value: c.code, label: `${c.code} — ${c.name}` });
  const featured = currencies.filter((c) => c.featured);
  const others = currencies.filter((c) => !c.featured);
  return featured.length
    ? [{ label: 'Popular', options: featured.map(option) }, { label: 'All currencies', options: others.map(option) }]
    : currencies.map(option);
};

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
  const fromSelect = select(currencyOptions(data.currencies), data.defaultFrom);
  const toSelect = select(currencyOptions(data.currencies), data.defaultTo);
  const panel = createResultPanel({ placeholder: 'Enter an amount, then convert.' });
  const footnote = h('p', { class: 'card-foot', title: provenanceText(data.rates) }, provenanceText(data.rates));

  const form = h('form', { class: 'form', novalidate: true },
    field('Amount', amountInput).el,
    h('div', { class: 'form-row units' },
      field('From', fromSelect).el,
      swapButton('Swap currencies', () => {
        [fromSelect.value, toSelect.value] = [toSelect.value, fromSelect.value];
        calculator.refresh();
      }),
      field('To', toSelect).el),
    h('button', { type: 'submit', class: 'btn btn-primary' }, 'Convert'));

  const calculator = bindCalculator({
    form,
    panel,
    buildRequest: () => ({ amount: readNumber(amountInput, 'an amount'), from: fromSelect.value, to: toSelect.value }),
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
