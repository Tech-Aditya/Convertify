/**
 * Minimal DOM helpers and reusable form controls. Every control is labelled for screen readers.
 */
let idCounter = 0;
export const uid = (prefix = 'cv') => `${prefix}-${++idCounter}`;

/** h('div', { class: 'x', onclick: fn }, child, 'text') */
export function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs ?? {})) {
    if (value === undefined || value === null || value === false) continue;
    if (key.startsWith('on') && typeof value === 'function') {
      el.addEventListener(key.slice(2), value);
    } else if (key === 'class') {
      el.className = value;
    } else if (key === 'dataset') {
      Object.assign(el.dataset, value);
    } else if (key in el && typeof value !== 'string') {
      el[key] = value;
    } else {
      el.setAttribute(key, value === true ? '' : value);
    }
  }
  append(el, children);
  return el;
}

function append(el, children) {
  for (const child of children.flat()) {
    if (child === null || child === undefined || child === false) continue;
    el.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
}

export const svgIcon = (pathD) => {
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('aria-hidden', 'true');
  const path = document.createElementNS(ns, 'path');
  path.setAttribute('d', pathD);
  svg.append(path);
  return svg;
};

/** A labelled field wrapper. Returns { el, control }. */
export function field(label, control, { hint, hideLabel = false } = {}) {
  control.id ||= uid('field');
  const hintEl = hint ? h('span', { class: 'field-hint', id: uid('hint') }, hint) : null;
  if (hintEl) control.setAttribute('aria-describedby', hintEl.id);
  const el = h('div', { class: 'field' },
    h('label', { class: hideLabel ? 'field-label visually-hidden' : 'field-label', for: control.id }, label),
    control,
    hintEl);
  return { el, control };
}

export function numberInput({ value = '', placeholder = '0', min, name } = {}) {
  return h('input', {
    class: 'input', type: 'text', inputmode: 'decimal', autocomplete: 'off', spellcheck: 'false',
    value: String(value), placeholder, name, 'data-min': min
  });
}

export function dateInput({ value = '' } = {}) {
  return h('input', { class: 'input', type: 'date', value, min: '0001-01-01', max: '9999-12-31' });
}

/**
 * Select built from options [{ value, label }] or groups [{ label, options: [...] }].
 */
export function select(items = [], selected) {
  const el = h('select', { class: 'select' });
  setOptions(el, items, selected);
  return el;
}

export function setOptions(selectEl, items, selected) {
  const previous = selected ?? selectEl.value;
  selectEl.replaceChildren(...items.map((item) => item.options
    ? h('optgroup', { label: item.label }, ...item.options.map(optionEl))
    : optionEl(item)));
  const values = [...selectEl.options].map((o) => o.value);
  if (values.includes(previous)) selectEl.value = previous;
}

const optionEl = ({ value, label }) => h('option', { value }, label);

/** Accessible segmented control built on radio inputs. Returns { el, get value(), onChange(fn) }. */
export function segmented(name, options, selected, ariaLabel) {
  const groupName = uid(name);
  const listeners = [];
  const el = h('div', { class: 'segmented', role: 'radiogroup', 'aria-label': ariaLabel },
    ...options.map(({ value, label }) => h('label', {},
      h('input', {
        type: 'radio', name: groupName, value, checked: value === selected,
        onchange: () => listeners.forEach((fn) => fn(value))
      }),
      h('span', {}, label))));
  return {
    el,
    get value() { return el.querySelector('input:checked')?.value; },
    onChange(fn) { listeners.push(fn); }
  };
}

export const SWAP_ICON = 'M7 4 3 8l4 4M3 8h14M17 20l4-4-4-4M21 16H7';

export function swapButton(label, onClick) {
  return h('button', { type: 'button', class: 'swap-btn', 'aria-label': label, title: label, onclick: onClick },
    svgIcon(SWAP_ICON), h('span', { 'aria-hidden': 'true' }, 'Swap'));
}

/** Loading placeholder shown while a card's options load. */
export function skeleton(rows = 3) {
  return h('div', { class: 'skeleton', 'aria-hidden': 'true' }, ...Array.from({ length: rows }, () => h('span')));
}

/** Card-level failure (e.g. options could not load) with a retry action. */
export function cardError(message, onRetry) {
  return h('div', { class: 'card-error', role: 'alert' },
    h('p', {}, message),
    h('button', { type: 'button', class: 'btn btn-ghost', onclick: onRetry }, 'Try again'));
}

export function debounce(fn, delay) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}
