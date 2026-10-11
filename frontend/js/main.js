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

/** Highlight the header link of the tool currently in view. */
function initScrollSpy() {
  const links = new Map([...document.querySelectorAll('.section-nav a[href^="#"]')]
    .map((link) => [link.getAttribute('href').slice(1), link]));
  if (!links.size || !('IntersectionObserver' in window)) return;

  const nav = document.querySelector('.section-nav');
  const visible = new Set();
  let shown;
  let pinnedUntil = 0;

  function setCurrent(current) {
    for (const [id, link] of links) {
      if (id === current) link.setAttribute('aria-current', 'true');
      else link.removeAttribute('aria-current');
    }
    // On narrow screens the links are a swipeable row: keep the current one in view. Only the
    // row scrolls (not scrollIntoView, which can interrupt the page's own scrolling).
    if (current && current !== shown && nav.scrollWidth > nav.clientWidth) {
      const link = links.get(current).getBoundingClientRect();
      const row = nav.getBoundingClientRect();
      nav.scrollTo({ left: nav.scrollLeft + link.left - row.left - (row.width - link.width) / 2, behavior: 'smooth' });
    }
    shown = current;
  }

  // First tool (in page order) that is inside the reading band wins.
  const fromScroll = () => {
    if (performance.now() >= pinnedUntil) setCurrent([...links.keys()].find((id) => visible.has(id)));
  };

  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) visible.add(entry.target.id);
      else visible.delete(entry.target.id);
    }
    fromScroll();
  }, { rootMargin: '-35% 0px -55% 0px' });

  // A tapped link highlights at once and stays highlighted while the page scrolls past the
  // tools in between, instead of flickering through them.
  for (const [id, link] of links) {
    link.addEventListener('click', () => {
      pinnedUntil = performance.now() + 1000;
      setCurrent(id);
      setTimeout(fromScroll, 1050);
    });
  }

  for (const id of links.keys()) {
    const section = document.getElementById(id);
    if (section) observer.observe(section);
  }
}
initScrollSpy();
