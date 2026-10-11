/**
 * Thin client for the Convertify REST API.
 * - Reads the base URL and timeout from window.CONVERTIFY_CONFIG.
 * - Turns API error bodies into ApiError with the server's user-safe message.
 * - Memoises GET requests and recent identical POSTs so repeated conversions cost no round trip.
 */
const config = window.CONVERTIFY_CONFIG ?? {};
const BASE_URL = (config.apiBaseUrl ?? '').replace(/\/$/, '') + '/api/v1';
const TIMEOUT_MS = config.requestTimeoutMs ?? 90000;
const SLOW_NOTICE_MS = config.slowNoticeMs ?? 3000;
const POST_CACHE_LIMIT = 100;

/**
 * Number of requests that have been waiting longer than SLOW_NOTICE_MS. While it is above zero
 * the server is most likely starting up (free hosting sleeps when idle); listeners of the
 * `convertify:server-slow` window event get { detail: { slow: boolean } } on every change.
 */
let slowRequests = 0;
function setSlow(delta) {
  const before = slowRequests > 0;
  slowRequests += delta;
  if (before !== slowRequests > 0) {
    window.dispatchEvent(new CustomEvent('convertify:server-slow', { detail: { slow: slowRequests > 0 } }));
  }
}

export class ApiError extends Error {
  constructor(message, { status = 0, code = 'NETWORK_ERROR', violations = [] } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.violations = violations;
  }
}

const getCache = new Map();
const postCache = new Map();

async function request(path, { method = 'GET', body } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  let slow = false;
  const slowTimer = setTimeout(() => { slow = true; setSlow(1); }, SLOW_NOTICE_MS);
  let response;
  try {
    response = await fetch(BASE_URL + path, {
      method,
      headers: body ? { 'Content-Type': 'application/json', Accept: 'application/json' } : { Accept: 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal
    });
  } catch (error) {
    const message = error.name === 'AbortError'
      ? 'The server is taking longer than usual to start. Please try again in a moment.'
      : 'Unable to reach the Convertify server. Check your connection and try again.';
    throw new ApiError(message);
  } finally {
    clearTimeout(timer);
    clearTimeout(slowTimer);
    if (slow) setSlow(-1);
  }

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    // Non-JSON body (e.g. proxy error page); handled below.
  }
  if (!response.ok) {
    throw new ApiError(payload?.message ?? `Request failed (${response.status}). Please try again.`, {
      status: response.status,
      code: payload?.error ?? 'HTTP_ERROR',
      violations: payload?.violations ?? []
    });
  }
  return payload;
}

/** GET with an in-memory cache for the page lifetime (catalogs rarely change). Failures are not cached. */
export function get(path, { fresh = false } = {}) {
  if (fresh) getCache.delete(path);
  if (!getCache.has(path)) {
    const promise = request(path).catch((error) => {
      getCache.delete(path);
      throw error;
    });
    getCache.set(path, promise);
  }
  return getCache.get(path);
}

/** POST; identical successful requests are answered from a small LRU cache. */
export async function post(path, body, { cache = true } = {}) {
  const key = path + '|' + JSON.stringify(body);
  if (cache && postCache.has(key)) {
    const value = postCache.get(key);
    postCache.delete(key);
    postCache.set(key, value);
    return value;
  }
  const result = await request(path, { method: 'POST', body });
  if (cache) {
    postCache.set(key, result);
    if (postCache.size > POST_CACHE_LIMIT) postCache.delete(postCache.keys().next().value);
  }
  return result;
}

export const api = {
  unitCatalog: () => get('/units'),
  convertUnits: (body) => post('/units/convert', body),
  convertAllUnits: (body) => post('/units/convert-all', body),
  currencies: (fresh = false) => get('/currency/currencies', { fresh }),
  // Rates change over time, so currency results are not memoised.
  convertCurrency: (body) => post('/currency/convert', body, { cache: false }),
  landRegions: () => get('/area/regions'),
  convertLand: (body) => post('/area/convert', body),
  convertAllLand: (body) => post('/area/convert-all', body),
  travelOptions: () => get('/time-distance/options'),
  calculateTravel: (body) => post('/time-distance/calculate', body),
  dateOptions: () => get('/date/options'),
  // "Today" depends on the clock, so date shifts are not memoised.
  shiftDate: (body) => post('/date/shift', body, { cache: false }),
  dateDifference: (body) => post('/date/difference', body)
};
