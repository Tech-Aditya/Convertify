import { debounce } from '../dom.js';
import { parseDecimal } from '../format.js';

const LIVE_DELAY = window.CONVERTIFY_CONFIG?.liveUpdateDelayMs ?? 350;

/** A problem with the user's input detected before calling the API. */
export class ValidationError extends Error {
  constructor(message, control, { empty = false } = {}) {
    super(message);
    this.control = control;
    this.empty = empty;
  }
}

/**
 * Reads a decimal from an input. Throws ValidationError with a readable message when empty or
 * not a number; sign/range rules are enforced by the API so there is one source of truth.
 */
export function readNumber(input, label) {
  if (!input.value.trim()) throw new ValidationError(`Enter ${label}.`, input, { empty: true });
  const value = parseDecimal(input.value);
  if (value === null) throw new ValidationError('Please enter a valid number, e.g. 12.5', input);
  return value;
}

export function readDate(input, label) {
  if (!input.value) throw new ValidationError(`Choose ${label}.`, input, { empty: true });
  return input.value;
}

/**
 * Shared behaviour for calculator forms:
 * - submit via the button or Enter;
 * - after the first calculation, edits recalculate live (debounced);
 * - out-of-order responses are discarded so the panel always matches the latest input.
 */
export function bindCalculator({ form, panel, buildRequest, execute, render }) {
  let sequence = 0;
  let engaged = false;
  let invalidControl = null;

  async function run({ live = false } = {}) {
    const current = ++sequence;
    invalidControl?.removeAttribute('aria-invalid');
    invalidControl = null;

    let request;
    try {
      request = buildRequest();
    } catch (error) {
      if (!(error instanceof ValidationError)) throw error;
      if (live && error.empty) {
        // Clearing a field while typing shouldn't flash an error.
        panel.showEmpty();
        return;
      }
      if (error.control) {
        invalidControl = error.control;
        invalidControl.setAttribute('aria-invalid', 'true');
      }
      panel.showError(error.message);
      return;
    }

    panel.showLoading();
    try {
      const response = await execute(request);
      if (current !== sequence) return;
      render(response, request);
    } catch (error) {
      if (current !== sequence) return;
      panel.showError(error.message || 'Something went wrong. Please try again.');
    }
  }

  const live = debounce(() => {
    if (engaged) run({ live: true });
  }, LIVE_DELAY);

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    engaged = true;
    run();
  });
  form.addEventListener('input', live);
  form.addEventListener('change', live);

  return {
    run,
    /** Recalculate if the user has already calculated once (e.g. after options change). */
    refresh: () => engaged && run({ live: true })
  };
}
