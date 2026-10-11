/**
 * Locale-aware formatting. Numbers arrive from the API as exact decimal strings; Intl formats
 * them for grouping without re-introducing floating-point noise.
 */
const locale = navigator.language || 'en-US';

const plainNumber = new Intl.NumberFormat(locale, { maximumFractionDigits: 20 });

/** "1234567.5" → "1,234,567.5" (exact). Falls back to the raw string if unsupported. */
export function formatNumber(decimalString) {
  if (decimalString === null || decimalString === undefined || decimalString === '') return '';
  try {
    return plainNumber.format(decimalString);
  } catch {
    return String(decimalString);
  }
}

export function formatCurrency(decimalString, currencyCode, fractionDigits) {
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currencyCode,
      currencyDisplay: 'narrowSymbol',
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits
    }).format(decimalString);
  } catch {
    return `${formatNumber(decimalString)} ${currencyCode}`;
  }
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const dateTimeFormat = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' });

/** "2026-12-18" → "18 Dec 2026" (DD MMM YYYY; calendar date, no time-zone shifting). */
export function formatDate(isoDate) {
  const [year, month, day] = isoDate.split('-');
  return `${day} ${MONTHS[Number(month) - 1]} ${Number(year)}`;
}

export function formatDateTime(isoInstant) {
  return isoInstant ? dateTimeFormat.format(new Date(isoInstant)) : '';
}

export const plural = (count, noun) => `${formatNumber(String(count))} ${noun}${Number(count) === 1 ? '' : 's'}`;

/**
 * Normalises user input into a decimal string for the API: trims, drops grouping separators
 * and spaces, accepts a leading minus. Returns null when the text is not a number.
 */
export function parseDecimal(text) {
  const cleaned = String(text ?? '').trim().replace(/[\s,_]/g, '');
  if (!/^[-+]?(\d+\.?\d*|\.\d+)$/.test(cleaned)) return null;
  return cleaned.replace(/^\+/, '');
}
