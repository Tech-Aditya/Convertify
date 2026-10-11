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
  requestTimeoutMs: 15000,
  liveUpdateDelayMs: 350
});
