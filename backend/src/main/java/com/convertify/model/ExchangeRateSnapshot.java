package com.convertify.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * A provider-neutral set of exchange rates: one unit of {@code baseCurrency} equals
 * {@code rates.get(code)} units of {@code code}.
 *
 * @param lastUpdated when the provider last published the rates (if known)
 * @param nextUpdate  when the provider expects to publish again (if known)
 * @param fetchedAt   when Convertify retrieved them
 */
public record ExchangeRateSnapshot(
        String baseCurrency,
        Map<String, BigDecimal> rates,
        Instant lastUpdated,
        Instant nextUpdate,
        Instant fetchedAt,
        String source) {

    public ExchangeRateSnapshot {
        rates = Map.copyOf(rates);
    }
}
