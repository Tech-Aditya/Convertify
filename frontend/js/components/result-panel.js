import { h, svgIcon } from '../dom.js';

const ICONS = {
  copy: 'M9 9.5h9.5v10H9zM15 9.5V5.5H5.5v10H9',
  check: 'm5 12.5 4.5 4.5L19 7.5',
  arrow: 'M12 5v14m0 0-5-5m5 5 5-5',
  empty: 'M12 3.5 20.5 12 12 20.5 3.5 12z',
  alert: 'M12 8v5m0 3.2v.3'
};

/**
 * The result component shared by every calculator. States:
 * - empty:   compact invitation ("Your result") with an optional hint line;
 * - loading: previous content dimmed, spinner in the header;
 * - success: source quantity → large primary result → "Source → Target" caption,
 *            then secondary details and notes;
 * - error:   clear message with an icon.
 * Updates are announced politely to screen readers.
 */
export function createResultPanel({ placeholder = 'Enter a value above to see the result here.' } = {}) {
  const label = h('span', { class: 'result-label' }, 'Result');
  const copyLabel = h('span', {}, 'Copy');
  const copyIcon = svgIcon(ICONS.copy);
  const copyBtn = h('button', { type: 'button', class: 'result-copy', hidden: true, title: 'Copy result to clipboard' },
    copyIcon, copyLabel);
  const loading = h('span', { class: 'result-loading' },
    h('span', { class: 'spinner', 'aria-hidden': 'true' }), 'Calculating');
  const body = h('div', { class: 'result-body' });
  const el = h('div', { class: 'result', 'aria-live': 'polite', 'data-state': 'empty' },
    h('div', { class: 'result-head' }, label, h('span', { class: 'result-actions' }, loading, copyBtn)),
    body);

  let copyText = '';
  let copyResetTimer;

  copyBtn.addEventListener('click', async () => {
    if (!copyText) return;
    let ok = true;
    try {
      await navigator.clipboard.writeText(copyText);
    } catch {
      ok = false;
    }
    copyLabel.textContent = ok ? 'Copied' : 'Copy failed';
    copyIcon.replaceWith(svgIcon(ok ? ICONS.check : ICONS.copy));
    copyBtn.dataset.copied = String(ok);
    clearTimeout(copyResetTimer);
    copyResetTimer = setTimeout(() => {
      copyLabel.textContent = 'Copy';
      copyBtn.querySelector('svg').replaceWith(copyIcon);
      delete copyBtn.dataset.copied;
    }, 1600);
  });

  function setState(state) {
    el.dataset.state = state;
    el.setAttribute('aria-busy', String(state === 'loading'));
    label.textContent = state === 'error' ? 'Unable to calculate' : 'Result';
  }

  function hideCopy() {
    copyText = '';
    copyBtn.hidden = true;
  }

  /** Empty state; an optional hint adds a smaller line (e.g. a region's conversion basis). */
  function showEmpty(text = placeholder, hint) {
    setState('empty');
    hideCopy();
    const nodes = [
      h('span', { class: 'result-empty-icon', 'aria-hidden': 'true' }, svgIcon(ICONS.empty)),
      h('p', { class: 'result-empty-title' }, 'Your result'),
      h('p', { class: 'result-empty-text' }, text)
    ];
    if (hint) nodes.push(h('p', { class: 'result-hint', title: hint }, hint));
    body.replaceChildren(h('div', { class: 'result-empty' }, ...nodes));
  }

  function showLoading() {
    // The previous content stays (dimmed) so the layout doesn't jump.
    el.dataset.state = el.dataset.state === 'empty' ? 'loading-empty' : 'loading';
    el.setAttribute('aria-busy', 'true');
  }

  /**
   * @param from    source quantity shown above the result, e.g. "180 cm" (optional)
   * @param value   primary figure
   * @param unit    unit shown beside the primary figure (optional)
   * @param caption line under the figure, e.g. "Centimeter → Meter" (optional)
   * @param detail  secondary line(s), e.g. the unit rate (optional)
   * @param meta    notes / provenance nodes shown in a separated footer (optional)
   * @param copy    clipboard text (defaults to value + unit)
   */
  function showResult({ from, value, unit, caption, detail, meta = [], copy }) {
    setState('success');
    copyText = copy ?? [value, unit].filter(Boolean).join(' ');
    copyBtn.hidden = !copyText;

    const flow = [];
    if (from) {
      flow.push(h('p', { class: 'result-from' }, from));
      flow.push(h('span', { class: 'result-arrow', 'aria-hidden': 'true' }, svgIcon(ICONS.arrow)));
    }
    flow.push(h('p', { class: 'result-value' }, h('span', { class: 'result-figure' }, value),
      unit ? h('span', { class: 'result-unit' }, unit) : null));
    if (caption) flow.push(h('p', { class: 'result-caption' }, caption));

    const nodes = [h('div', { class: 'result-flow' }, ...flow)];
    const details = [detail].flat().filter(Boolean);
    if (details.length) nodes.push(h('div', { class: 'result-details' }, ...details.map((line) => h('p', {}, line))));
    const metaNodes = [meta].flat().filter(Boolean);
    if (metaNodes.length) nodes.push(h('div', { class: 'result-meta' }, ...metaNodes));
    body.replaceChildren(...nodes);
  }

  function showError(message) {
    setState('error');
    hideCopy();
    body.replaceChildren(h('div', { class: 'result-error', role: 'alert' },
      h('span', { class: 'result-error-icon', 'aria-hidden': 'true' }, svgIcon(ICONS.alert)),
      h('p', { class: 'result-error-text' }, message)));
  }

  showEmpty();
  return { el, showEmpty, showLoading, showResult, showError };
}
