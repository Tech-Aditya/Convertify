/*
 * Runtime configuration for the Convertify frontend.
 *
 * apiBaseUrl — where the Convertify API lives.
 *   ''                        same origin (default: the backend serves this frontend)
 *   'http://localhost:8090'   when this folder is hosted separately (e.g. on port 5173)
 * Edit this file at deploy time; no rebuild is needed.
 */
window.CONVERTIFY_CONFIG = Object.freeze({
  apiBaseUrl: '',
  // Generous, because free hosting (Render) sleeps when idle and the first request after that
  // waits while the server starts, which can take about a minute.
  requestTimeoutMs: 90000,
  // After this long without a response, a "waking up the server" notice is shown.
  slowNoticeMs: 3000,
  liveUpdateDelayMs: 350
});
