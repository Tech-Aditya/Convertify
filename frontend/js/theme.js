/** Light/dark theme toggle, persisted in localStorage (the initial theme is applied inline in <head>). */
const STORAGE_KEY = 'convertify-theme';

export function initTheme(button) {
  const root = document.documentElement;

  const sync = () => {
    const dark = root.dataset.theme === 'dark';
    button.setAttribute('aria-label', dark ? 'Switch to light theme' : 'Switch to dark theme');
    button.setAttribute('aria-pressed', String(dark));
  };

  button.addEventListener('click', () => {
    const next = root.dataset.theme === 'dark' ? 'light' : 'dark';
    root.dataset.theme = next;
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Storage unavailable (private mode): the choice lasts for this page view.
    }
    sync();
  });

  // Follow OS changes until the user has chosen explicitly.
  window.matchMedia?.('(prefers-color-scheme: dark)').addEventListener?.('change', (event) => {
    let saved = null;
    try { saved = localStorage.getItem(STORAGE_KEY); } catch { /* ignore */ }
    if (!saved) {
      root.dataset.theme = event.matches ? 'dark' : 'light';
      sync();
    }
  });

  sync();
}
