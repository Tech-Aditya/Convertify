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

  const visible = new Set();
  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) visible.add(entry.target.id);
      else visible.delete(entry.target.id);
    }
    // First tool (in page order) that is inside the reading band wins.
    const current = [...links.keys()].find((id) => visible.has(id));
    for (const [id, link] of links) {
      if (id === current) link.setAttribute('aria-current', 'true');
      else link.removeAttribute('aria-current');
    }
  }, { rootMargin: '-35% 0px -55% 0px' });

  for (const id of links.keys()) {
    const section = document.getElementById(id);
    if (section) observer.observe(section);
  }
}
initScrollSpy();
