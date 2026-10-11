package com.convertify.exception;

/**
 * Technical failure talking to an exchange-rate provider. Internal only — the rate cache
 * translates it into a stale-data response or an {@link ExchangeRateUnavailableException}.
 */
public class ExchangeRateProviderException extends RuntimeException {

    public ExchangeRateProviderException(String message) {
        super(message);
    }

    public ExchangeRateProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
