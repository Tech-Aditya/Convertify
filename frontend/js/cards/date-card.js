import { api } from '../api.js';
import { cardError, dateInput, field, h, numberInput, segmented, select, skeleton, uid } from '../dom.js';
import { formatDate, formatNumber, plural } from '../format.js';
import { ValidationError, bindCalculator, readDate } from '../components/calculator.js';
import { createResultPanel } from '../components/result-panel.js';

const UNITS = [
  { value: 'DAYS', label: 'Days', noun: 'day' },
  { value: 'WEEKS', label: 'Weeks', noun: 'week' },
  { value: 'MONTHS', label: 'Months', noun: 'month' },
  { value: 'YEARS', label: 'Years', noun: 'year' },
  { value: 'BUSINESS_DAYS', label: 'Business days', noun: 'business day' }
];
const NO_HOLIDAYS = '';

function calendarControls(options) {
  const weekend = select(options.weekendPresets.map((p) => ({ value: p.code, label: p.label })), options.defaultWeekendPreset);
  const holidays = select([{ value: NO_HOLIDAYS, label: 'No holidays' },
    ...options.holidayCalendars.map((c) => ({ value: c.code, label: c.name }))], NO_HOLIDAYS);
  const el = h('div', { class: 'form-row' },
    field('Weekend', weekend).el,
    field('Holidays', holidays, { hint: 'Holidays are skipped like weekends.' }).el);
  return { el, weekend, holidays };
}

const calendarLine = (calendar) => `Weekend: ${calendar.weekend} · Holidays: ${calendar.holidays}`;

function readWholeNumber(input, label, max) {
  if (!input.value.trim()) throw new ValidationError(`Enter ${label}.`, input, { empty: true });
  const text = input.value.trim().replace(/[\s,_]/g, '');
  if (!/^\d+$/.test(text)) throw new ValidationError('Amount must be a whole number (0 or more).', input);
  if (Number(text) > max) throw new ValidationError(`Amount cannot exceed ${formatNumber(String(max))}.`, input);
  return Number(text);
}

function buildShiftTab(options) {
  const useToday = h('input', { type: 'checkbox', checked: true });
  const startInput = dateInput({ value: options.today });
  startInput.disabled = true;
  startInput.setAttribute('aria-label', 'Start date');
  const operation = segmented('op', [{ value: 'ADD', label: 'Add' }, { value: 'SUBTRACT', label: 'Subtract' }], 'ADD', 'Operation');
  const amountInput = numberInput({ placeholder: 'e.g. 80' });
  const unitSelect = select(UNITS, 'DAYS');
  const calendar = calendarControls(options);
  calendar.el.hidden = true;
  const panel = createResultPanel({ placeholder: 'Pick a date and amount, then calculate.' });

  useToday.addEventListener('change', () => {
    startInput.disabled = useToday.checked;
    if (useToday.checked) startInput.value = options.today;
    else startInput.focus();
  });
  unitSelect.addEventListener('change', () => {
    calendar.el.hidden = unitSelect.value !== 'BUSINESS_DAYS';
  });

  const form = h('form', { class: 'form', novalidate: true },
    h('div', { class: 'field' },
      h('span', { class: 'field-label' }, 'Start date'),
      h('div', { class: 'input-group with-toggle' },
        startInput,
        h('label', { class: 'checkbox' }, useToday, 'Today'))),
    h('div', { class: 'field' },
      h('span', { class: 'field-label', 'aria-hidden': 'true' }, 'Operation'),
      operation.el),
    h('div', { class: 'form-row' },
      field('Amount', amountInput).el,
      field('Unit', unitSelect).el),
    calendar.el,
    h('button', { type: 'submit', class: 'btn btn-primary' }, 'Calculate date'));

  bindCalculator({
    form,
    panel,
    buildRequest: () => {
      const business = unitSelect.value === 'BUSINESS_DAYS';
      return {
        date: useToday.checked ? undefined : readDate(startInput, 'a start date'),
        operation: operation.value,
        amount: readWholeNumber(amountInput, 'an amount', options.maxShiftAmount),
        unit: unitSelect.value,
        weekendPreset: business ? calendar.weekend.value : undefined,
        holidayCalendar: business && calendar.holidays.value ? calendar.holidays.value : undefined
      };
    },
    execute: api.shiftDate,
    render: (response) => {
      const noun = UNITS.find((u) => u.value === response.unit).noun;
      const direction = response.operation === 'ADD'
        ? (response.startIsToday ? 'from today' : `after ${formatDate(response.startDate)}`)
        : (response.startIsToday ? 'before today' : `before ${formatDate(response.startDate)}`);
      const resultText = formatDate(response.resultDate);
      const meta = [];
      if (response.unit === 'BUSINESS_DAYS') {
        meta.push(h('span', {}, `${plural(response.calendarDays, 'calendar day')} · ${plural(response.nonWorkingDaysSkipped, 'non-working day')} skipped`));
        meta.push(h('span', {}, calendarLine(response.calendar)));
      } else if (response.unit !== 'DAYS') {
        meta.push(h('span', {}, `${plural(response.calendarDays, 'calendar day')} ${response.operation === 'ADD' ? 'later' : 'earlier'}`));
      }
      const start = response.startIsToday ? 'Today' : formatDate(response.startDate);
      const sign = response.operation === 'ADD' ? '+' : '−';
      panel.showResult({
        from: `${start} ${sign} ${plural(response.amount, noun)}`,
        value: resultText,
        caption: response.resultDayOfWeek,
        detail: `${plural(response.amount, noun)} ${direction}`,
        meta,
        copy: resultText
      });
    }
  });

  return h('div', { class: 'date-layout' }, form, panel.el);
}

function buildDifferenceTab(options) {
  const startInput = dateInput({ value: options.today });
  const endInput = dateInput();
  const includeEnd = h('input', { type: 'checkbox' });
  const calendar = calendarControls(options);
  const panel = createResultPanel({ placeholder: 'Pick two dates, then calculate.' });

  const form = h('form', { class: 'form', novalidate: true },
    h('div', { class: 'form-row' },
      field('Start date', startInput).el,
      field('End date', endInput).el),
    h('label', { class: 'checkbox' }, includeEnd, 'Include the end date'),
    h('details', { class: 'disclosure' },
      h('summary', {}, 'Business-day options'),
      h('div', { class: 'disclosure-body' }, calendar.el)),
    h('button', { type: 'submit', class: 'btn btn-primary' }, 'Calculate difference'));

  bindCalculator({
    form,
    panel,
    buildRequest: () => {
      const startDate = readDate(startInput, 'a start date');
      const endDate = readDate(endInput, 'an end date');
      if (endDate < startDate) throw new ValidationError('End date must be on or after the start date.', endInput);
      return {
        startDate,
        endDate,
        includeEndDate: includeEnd.checked,
        weekendPreset: calendar.weekend.value,
        holidayCalendar: calendar.holidays.value || undefined
      };
    },
    execute: api.dateDifference,
    render: (r) => {
      const weeks = [r.weeks ? plural(r.weeks, 'week') : null, r.remainingDays || !r.weeks ? plural(r.remainingDays, 'day') : null]
        .filter(Boolean).join(' ');
      const period = [r.years ? plural(r.years, 'year') : null, r.months ? plural(r.months, 'month') : null, r.days ? plural(r.days, 'day') : null]
        .filter(Boolean).join(' ');
      const detail = [];
      if ((r.years || r.months) && period) detail.push(`Calendar period: ${period}`);
      if (r.includeEndDate) detail.push('End date included');
      panel.showResult({
        from: `${formatDate(r.startDate)} → ${formatDate(r.endDate)}`,
        value: formatNumber(String(r.totalDays)),
        unit: Number(r.totalDays) === 1 ? 'day' : 'days',
        caption: r.totalDays >= 7 ? `Approximately ${weeks}` : null,
        detail,
        meta: [h('span', {}, `${plural(r.businessDays, 'business day')} · ${calendarLine(r.calendar)}`)],
        copy: plural(r.totalDays, 'day')
      });
    }
  });

  return h('div', { class: 'date-layout' }, form, panel.el);
}

/** Accessible tabs: arrow keys move between tabs, only the active tab is in the tab order. */
function tabs(definitions) {
  const tabButtons = [];
  const panels = [];
  const list = h('div', { class: 'tabs', role: 'tablist', 'aria-label': 'Date calculator mode' });

  definitions.forEach(({ label, content }, index) => {
    const tabId = uid('tab');
    const panelId = uid('panel');
    const button = h('button', {
      type: 'button', class: 'tab', role: 'tab', id: tabId,
      'aria-controls': panelId, 'aria-selected': String(index === 0), tabindex: index === 0 ? '0' : '-1'
    }, label);
    const panel = h('div', { class: 'tabpanel', role: 'tabpanel', id: panelId, 'aria-labelledby': tabId, hidden: index !== 0 }, content);
    button.addEventListener('click', () => activate(index));
    button.addEventListener('keydown', (event) => {
      const delta = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
      if (!delta) return;
      event.preventDefault();
      const next = (index + delta + definitions.length) % definitions.length;
      activate(next);
      tabButtons[next].focus();
    });
    tabButtons.push(button);
    panels.push(panel);
    list.append(button);
  });

  function activate(active) {
    tabButtons.forEach((button, i) => {
      button.setAttribute('aria-selected', String(i === active));
      button.tabIndex = i === active ? 0 : -1;
      panels[i].hidden = i !== active;
    });
  }

  return h('div', { class: 'tabset' }, list, ...panels);
}

export async function mountDateCard(container) {
  container.replaceChildren(skeleton(4));
  let options;
  try {
    options = await api.dateOptions();
  } catch (error) {
    container.replaceChildren(cardError(error.message, () => mountDateCard(container)));
    return;
  }

  container.replaceChildren(tabs([
    { label: 'Add / Subtract', content: buildShiftTab(options) },
    { label: 'Difference', content: buildDifferenceTab(options) }
  ]));
}
