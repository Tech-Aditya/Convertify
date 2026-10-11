package com.convertify.exception;

/** Exchange-rate data could not be obtained and no cached copy is available. */
public class ExchangeRateUnavailableException extends ConvertifyException {

    public ExchangeRateUnavailableException(String message) {
        super(ErrorCode.EXCHANGE_RATE_UNAVAILABLE, message);
    }
}
