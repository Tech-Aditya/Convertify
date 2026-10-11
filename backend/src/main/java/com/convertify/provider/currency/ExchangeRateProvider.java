package com.convertify.provider.currency;

import com.convertify.exception.ExchangeRateProviderException;
import com.convertify.model.ExchangeRateSnapshot;

/**
 * Source of exchange rates. Business logic depends only on this abstraction, so a different
 * provider (another API, a database, a file feed) can be plugged in by adding an implementation
 * and selecting it with {@code convertify.currency.provider.type}.
 */
public interface ExchangeRateProvider {

    /**
     * Fetches the latest rates relative to {@code baseCurrency}.
     *
     * @throws ExchangeRateProviderException when the provider is unreachable or returns unusable data
     */
    ExchangeRateSnapshot fetchLatestRates(String baseCurrency);

    /** Display name of the data source, shown to users for attribution. */
    String name();
}
