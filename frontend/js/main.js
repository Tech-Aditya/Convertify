import { h } from './dom.js';
import { initTheme } from './theme.js';
import { mountUnitCard } from './cards/unit-card.js';
import { mountCurrencyCard } from './cards/currency-card.js';
import { mountTravelCard } from './cards/travel-card.js';
import { mountLandCard } from './cards/land-card.js';
import { mountDateCard } from './cards/date-card.js';

const MOUNTERS = {
  unit: mountUnitCard,
  currency: mountCurrencyCard,
  travel: mountTravelCard,
  land: mountLandCard,
  date: mountDateCard
};

initTheme(document.getElementById('theme-toggle'));
initWakeNotice();

/**
 * Free hosting sleeps when idle, and the page itself may come from the browser cache, so the
 * first requests can wait about a minute while the server starts. Explain the wait instead of
 * leaving every card spinning silently.
 */
function initWakeNotice() {
  const notice = h('div', { class: 'wake-notice', role: 'status', hidden: true },
    h('span', { class: 'spinner', 'aria-hidden': 'true' }),
    h('span', {},
      h('strong', {}, 'Waking up the server… '),
      'Convertify sleeps when nobody is using it, so the first load can take up to a minute.'));
  document.body.append(notice);
  window.addEventListener('convertify:server-slow', (event) => {
    notice.hidden = !event.detail.slow;
  });
}
document.getElementById('year').textContent = String(new Date().getFullYear());

// Cards load independently: one failing (e.g. exchange rates) never blocks the others.
for (const container of document.querySelectorAll('[data-mount]')) {
  MOUNTERS[container.dataset.mount]?.(container);
}

/**
 * Sidebar listing every tool, grouped like the page. Built from the cards themselves (title,
 * subtitle, icon, accent colour), so it always matches what is on the page.
 */
function buildDrawer() {
  const drawer = document.getElementById('tools-drawer');
  const nav = drawer.querySelector('.drawer-nav');
  for (const section of document.querySelectorAll('.tool-section')) {
    const cards = [...section.querySelectorAll('article.card[id]')];
    nav.append(
      h('p', { class: 'drawer-group' }, section.querySelector('.section-head h2').textContent),
      h('ul', {}, ...cards.map((card) => h('li', {},
        h('a', { class: 'drawer-link', href: `#${card.id}`, dataset: { accent: card.dataset.accent } },
          h('span', { class: 'drawer-icon', 'aria-hidden': 'true' }, card.querySelector('.card-icon svg').cloneNode(true)),
          h('span', { class: 'drawer-text' },
            h('span', { class: 'drawer-title' }, card.querySelector('h3').textContent),
            h('span', { class: 'drawer-sub' }, card.querySelector('.card-subtitle').textContent)))))));
  }
  return drawer;
}

/** Opens and closes the sidebar; picking a tool closes it and scrolls to that card. */
function initDrawer(drawer, spy) {
  const button = document.getElementById('menu-button');
  button.addEventListener('click', () => {
    drawer.showModal();
    // Start at the tool currently in view rather than the top of the list.
    drawer.querySelector('[aria-current="true"]')?.scrollIntoView({ block: 'nearest' });
  });
  drawer.querySelector('.drawer-close').addEventListener('click', () => drawer.close());
  // A click on the backdrop targets the dialog element itself.
  drawer.addEventListener('click', (event) => {
    if (event.target === drawer) drawer.close();
  });
  drawer.querySelector('.drawer-nav').addEventListener('click', (event) => {
    const link = event.target.closest('a.drawer-link');
    if (!link) return;
    event.preventDefault();
    const id = link.getAttribute('href').slice(1);
    const card = document.getElementById(id);
    drawer.close();
    spy?.pin(id);
    card.scrollIntoView({ block: 'start' }); // honours scroll-padding (sticky header) and reduced motion
    history.replaceState(null, '', `#${id}`);
    // Move focus to the card for keyboard and screen-reader users, without a second scroll.
    card.setAttribute('tabindex', '-1');
    card.focus({ preventScroll: true });
  });
}

/** Highlights the tool currently in view in the header links and the sidebar. */
function initScrollSpy() {
  const links = new Map();
  for (const link of document.querySelectorAll('.section-nav a[href^="#"], .drawer-link')) {
    const id = link.getAttribute('href').slice(1);
    links.set(id, [...(links.get(id) ?? []), link]);
  }
  if (!links.size || !('IntersectionObserver' in window)) return null;

  const visible = new Set();
  let pinnedUntil = 0;

  function setCurrent(current) {
    for (const [id, group] of links) {
      for (const link of group) {
        if (id === current) link.setAttribute('aria-current', 'true');
        else link.removeAttribute('aria-current');
      }
    }
  }

  // First tool (in page order) that is inside the reading band wins.
  const fromScroll = () => {
    if (performance.now() >= pinnedUntil) setCurrent([...links.keys()].find((id) => visible.has(id)));
  };

  /** A chosen tool highlights at once and stays highlighted while the page scrolls past the
      tools in between, instead of flickering through them. */
  function pin(id) {
    pinnedUntil = performance.now() + 1000;
    setCurrent(id);
    setTimeout(fromScroll, 1050);
  }

  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) visible.add(entry.target.id);
      else visible.delete(entry.target.id);
    }
    fromScroll();
  }, { rootMargin: '-35% 0px -55% 0px' });

  for (const link of document.querySelectorAll('.section-nav a[href^="#"]')) {
    link.addEventListener('click', () => pin(link.getAttribute('href').slice(1)));
  }
  for (const id of links.keys()) {
    const section = document.getElementById(id);
    if (section) observer.observe(section);
  }
  return { pin };
}

const drawer = buildDrawer();
initDrawer(drawer, initScrollSpy());
