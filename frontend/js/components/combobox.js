import { h, svgIcon } from '../dom.js';

const CLEAR_ICON = 'M7 7l10 10M17 7 7 17';
let comboCounter = 0;

/** Lower-case and strip accents, so "sao tome" finds "São Tomé". */
const normalise = (text) => String(text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

/**
 * Lower is better; null means no match. Every word of the query has to match something.
 * Ranking: exact code, code prefix, a word of the name (or a symbol) starting with the query,
 * the name containing it, then the extra search keywords (country names, nicknames).
 */
function score(item, tokens) {
  let total = 0;
  for (const token of tokens) {
    let best = null;
    if (item.search.code === token) best = 0;
    else if (item.search.code.startsWith(token)) best = 1;
    else if (item.search.nameWords.some((word) => word.startsWith(token)) || item.search.symbols.includes(token)) best = 2;
    else if (item.search.name.includes(token)) best = 3;
    else if (item.search.keywordWords.some((word) => word.startsWith(token))) best = 4;
    else if (item.search.keywords.includes(token)) best = 5;
    if (best === null) return null;
    total += best;
  }
  return total;
}

/** "South Korean Won" with "kor" → ["South ", <mark>Kor</mark>, "ean Won"]. */
function highlight(text, query) {
  const index = query ? text.toLowerCase().indexOf(query) : -1;
  if (index < 0) return [text];
  return [text.slice(0, index), h('mark', {}, text.slice(index, index + query.length)), text.slice(index + query.length)];
}

/**
 * Searchable single-select, following the ARIA combobox pattern. The text field doubles as the
 * search box, so it behaves the same with a mouse, a keyboard and a phone's on-screen keyboard
 * (native <select> pickers on phones cannot be searched).
 *
 * @param items  [{ value, code, name, symbol?, keywords?, group? }]; `group` labels sections that
 *               are shown while the query is empty (e.g. "Popular")
 * @param noun   plural noun for announcements, e.g. "currencies"
 * Returns { el, input, value (get/set) }. Choosing an option dispatches a bubbling `change`
 * event from the input; typing a query does not bubble `input`/`change` to the form.
 */
export function createCombobox({ items, value, noun = 'options', searchHint = 'Search' }) {
  const id = `combo-${++comboCounter}`;
  const entries = items.map((item) => ({
    ...item,
    label: `${item.code} — ${item.name}`,
    search: {
      code: normalise(item.code),
      name: normalise(item.name),
      nameWords: normalise(item.name).split(/[\s\-()'.,]+/).filter(Boolean),
      symbols: [item.symbol].filter(Boolean).map(normalise),
      keywords: normalise(item.keywords),
      keywordWords: normalise(item.keywords).split(/\s+/).filter(Boolean)
    }
  }));
  const byValue = new Map(entries.map((entry) => [entry.value, entry]));

  let selected = byValue.get(value) ?? entries[0];
  let open = false;
  let visible = [];
  let activeIndex = -1;
  let dispatching = false;

  const input = h('input', {
    class: 'input combo-input', type: 'text', role: 'combobox', autocomplete: 'off', autocapitalize: 'off',
    autocorrect: 'off', spellcheck: 'false', enterkeyhint: 'done',
    'aria-autocomplete': 'list', 'aria-expanded': 'false', 'aria-controls': `${id}-list`
  });
  const clearBtn = h('button', { type: 'button', class: 'combo-clear', tabindex: '-1', 'aria-label': 'Clear search', hidden: true },
    svgIcon(CLEAR_ICON));
  const toggleBtn = h('button', { type: 'button', class: 'combo-toggle', tabindex: '-1', 'aria-label': `Show all ${noun}` });
  const list = h('ul', { class: 'combo-list', role: 'listbox', id: `${id}-list` });
  const empty = h('p', { class: 'combo-empty', hidden: true });
  const popup = h('div', { class: 'combo-popup', hidden: true }, list, empty);
  const status = h('span', { class: 'visually-hidden', role: 'status' });
  const el = h('div', { class: 'combo' }, input, clearBtn, toggleBtn, popup, status);

  const query = () => normalise(input.value);

  function showSelection() {
    input.value = selected.label;
    input.title = selected.label;
  }

  function render() {
    const q = query();
    const tokens = q.split(/\s+/).filter(Boolean);
    let groups;
    if (!tokens.length) {
      const order = [...new Set(entries.map((e) => e.group ?? ''))];
      groups = order.map((group) => [group, entries.filter((e) => (e.group ?? '') === group)]);
      visible = groups.flatMap(([, members]) => members);
    } else {
      visible = entries
        .map((entry, index) => ({ entry, index, rank: score(entry, tokens) }))
        .filter((r) => r.rank !== null)
        .sort((a, b) => a.rank - b.rank || a.index - b.index)
        .map((r) => r.entry);
      groups = [['', visible]];
    }

    const firstToken = tokens[0] ?? '';
    let optionIndex = 0;
    list.replaceChildren(...groups.flatMap(([group, members]) => [
      ...(group ? [h('li', { class: 'combo-group', role: 'presentation', 'aria-hidden': 'true' }, group)] : []),
      ...members.map((entry) => {
        const index = optionIndex++;
        return h('li', {
          class: 'combo-option', role: 'option', id: `${id}-opt-${index}`,
          'aria-selected': String(entry === selected), dataset: { index: String(index) }
        },
        h('span', { class: 'combo-code' }, ...highlight(entry.code, firstToken)),
        h('span', { class: 'combo-name' }, ...highlight(entry.name, firstToken)),
        entry.symbol && entry.symbol !== entry.code ? h('span', { class: 'combo-symbol', 'aria-hidden': 'true' }, entry.symbol) : null);
      })
    ]));

    empty.hidden = visible.length > 0;
    empty.textContent = `No ${noun} match “${input.value.trim()}”. Try a code (KRW), a name (won) or a country (Korea).`;
    clearBtn.hidden = !input.value || !open;
    status.textContent = tokens.length ? `${visible.length} ${visible.length === 1 ? 'result' : 'results'}` : '';
    setActive(tokens.length ? (visible.length ? 0 : -1) : visible.indexOf(selected), { scroll: 'center' });
  }

  function setActive(index, { scroll = 'nearest' } = {}) {
    list.querySelector('[data-active]')?.removeAttribute('data-active');
    activeIndex = index;
    const option = index >= 0 ? list.querySelector(`[data-index="${index}"]`) : null;
    if (option) {
      option.setAttribute('data-active', '');
      input.setAttribute('aria-activedescendant', option.id);
      if (scroll === 'center') {
        list.scrollTop = option.offsetTop - list.clientHeight / 2 + option.offsetHeight / 2;
      } else {
        option.scrollIntoView({ block: 'nearest' });
      }
    } else {
      input.removeAttribute('aria-activedescendant');
    }
  }

  /** Fits the list into the visible space (which shrinks when a phone keyboard opens). */
  function place() {
    if (!open) return;
    const viewport = window.visualViewport;
    const viewportTop = viewport?.offsetTop ?? 0;
    const viewportBottom = viewportTop + (viewport?.height ?? window.innerHeight);
    const header = parseFloat(getComputedStyle(document.documentElement).scrollPaddingTop) || 0;
    const rect = input.getBoundingClientRect();
    const below = viewportBottom - rect.bottom - 12;
    const above = rect.top - Math.max(header, viewportTop) - 12;
    const upward = below < 220 && above > below;
    popup.classList.toggle('is-above', upward);
    popup.style.maxHeight = `${Math.max(160, Math.min(340, upward ? above : below))}px`;
  }

  /** @param clear start with an empty search (false when text is already being typed) */
  function openList({ clear = true } = {}) {
    if (open) return;
    open = true;
    el.classList.add('is-open');
    input.setAttribute('aria-expanded', 'true');
    if (clear) input.value = '';
    input.placeholder = searchHint; // the current choice is highlighted in the list
    popup.hidden = false;
    // On touch screens, lift the field to just under the sticky header so the list has room
    // above the on-screen keyboard.
    if (window.matchMedia('(pointer: coarse)').matches) {
      const header = parseFloat(getComputedStyle(document.documentElement).scrollPaddingTop) || 0;
      const offset = input.getBoundingClientRect().top - header - 8;
      if (Math.abs(offset) > 24) window.scrollBy({ top: offset });
    }
    render();
    place();
  }

  function closeList() {
    if (!open) return;
    open = false;
    el.classList.remove('is-open');
    input.setAttribute('aria-expanded', 'false');
    input.removeAttribute('aria-activedescendant');
    input.placeholder = '';
    popup.hidden = true;
    clearBtn.hidden = true;
    status.textContent = '';
    showSelection();
  }

  function choose(entry) {
    const changed = entry !== selected;
    selected = entry;
    closeList();
    // Dismiss the on-screen keyboard after a tap; keep focus for keyboard users.
    if (window.matchMedia('(pointer: coarse)').matches) input.blur();
    if (changed) {
      dispatching = true;
      input.dispatchEvent(new Event('change', { bubbles: true }));
      dispatching = false;
    }
  }

  input.addEventListener('focus', openList);
  input.addEventListener('click', openList);
  // Typing into a focused but closed picker: open (clearing the selection's label) before the
  // text lands, so the first character becomes the search instead of being appended or lost.
  input.addEventListener('beforeinput', () => {
    if (!open) openList();
  });
  input.addEventListener('input', (event) => {
    event.stopPropagation(); // a search query is not a change of the form's value
    if (!open) openList({ clear: false });
    render();
  });
  input.addEventListener('change', (event) => {
    if (!dispatching) event.stopPropagation();
  });
  input.addEventListener('keydown', (event) => {
    const count = visible.length;
    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp': {
        event.preventDefault();
        if (!open) { openList(); return; }
        if (!count) return;
        const step = event.key === 'ArrowDown' ? 1 : -1;
        setActive(activeIndex < 0 ? (step > 0 ? 0 : count - 1) : (activeIndex + step + count) % count);
        break;
      }
      case 'PageDown':
      case 'PageUp':
        if (!open || !count) return;
        event.preventDefault();
        setActive(Math.min(count - 1, Math.max(0, activeIndex + (event.key === 'PageDown' ? 8 : -8))));
        break;
      case 'Enter':
        if (!open) return; // closed: Enter submits the form as usual
        event.preventDefault();
        if (activeIndex >= 0) choose(visible[activeIndex]);
        else if (!query()) closeList();
        break;
      case 'Escape':
        if (!open) return;
        event.preventDefault();
        if (input.value) { input.value = ''; render(); } else closeList();
        break;
      case 'Tab':
        if (open && query() && activeIndex >= 0) choose(visible[activeIndex]);
        else closeList();
        break;
      default:
    }
  });
  el.addEventListener('focusout', (event) => {
    if (!el.contains(event.relatedTarget)) closeList();
  });

  // Keep focus in the field while interacting with the list and buttons.
  popup.addEventListener('pointerdown', (event) => event.preventDefault());
  popup.addEventListener('pointermove', (event) => {
    const option = event.target.closest('.combo-option');
    if (option && Number(option.dataset.index) !== activeIndex) setActive(Number(option.dataset.index));
  });
  popup.addEventListener('click', (event) => {
    const option = event.target.closest('.combo-option');
    if (option) choose(visible[Number(option.dataset.index)]);
  });
  for (const button of [clearBtn, toggleBtn]) button.addEventListener('pointerdown', (event) => event.preventDefault());
  clearBtn.addEventListener('click', () => {
    input.value = '';
    render();
    input.focus();
  });
  toggleBtn.addEventListener('click', () => {
    if (open) {
      closeList();
    } else {
      input.focus();
      openList();
    }
  });

  window.visualViewport?.addEventListener('resize', place);
  window.addEventListener('resize', place);

  showSelection();
  return {
    el,
    input,
    get value() { return selected.value; },
    set value(next) {
      const entry = byValue.get(next);
      if (!entry) return;
      selected = entry;
      if (open) render(); else showSelection();
    }
  };
}
