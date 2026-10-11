package com.convertify.dto;

/**
 * Result of a currency conversion. Monetary values are decimal strings rounded to the target
 * currency's minor units; {@code rate} is 1 {@code from} in {@code to}.
 */
public record CurrencyConversionResponse(
        String amount,
        CurrencyDto from,
        CurrencyDto to,
        String result,
        String rate,
        String inverseRate,
        RateInfoDto rates) {
}
