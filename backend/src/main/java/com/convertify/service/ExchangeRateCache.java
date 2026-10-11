package com.convertify.service;

import com.convertify.config.CurrencyProperties;
import com.convertify.exception.ExchangeRateProviderException;
import com.convertify.exception.ExchangeRateUnavailableException;
import com.convertify.model.ExchangeRateSnapshot;
import com.convertify.provider.currency.ExchangeRateProvider;
import com.convertify.util.Durations;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Keeps the latest exchange-rate snapshot for a configurable TTL to limit provider calls.
 * If a refresh fails, the previous snapshot keeps being served (flagged as stale), and the provider
 * is not retried until the failure back-off has elapsed.
 */
@Component
public class ExchangeRateCache {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateCache.class);

    /** Rates plus whether they are older than the configured TTL. */
    public record Rates(ExchangeRateSnapshot snapshot, boolean stale) {
    }

    private final ExchangeRateProvider provider;
    private final CurrencyProperties properties;
    private final Clock clock;

    private volatile ExchangeRateSnapshot current;
    private volatile Instant lastFailureAt;

    public ExchangeRateCache(ExchangeRateProvider provider, CurrencyProperties properties, Clock clock) {
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
    }

    public Rates get() {
        ExchangeRateSnapshot snapshot = current;
        if (snapshot != null && isFresh(snapshot)) {
            log.debug("Using cached exchange rates (age {})", Durations.human(age(snapshot)));
            return new Rates(snapshot, false);
        }
        return refresh();
    }

    private synchronized Rates refresh() {
        ExchangeRateSnapshot snapshot = current;
        if (snapshot != null && isFresh(snapshot)) {
            return new Rates(snapshot, false);
        }
        if (inBackoff()) {
            log.debug("Skipping exchange-rate refresh: provider failed recently, backing off for {}",
                    Durations.human(properties.provider().failureBackoff()));
            return fallback();
        }
        log.info("Refreshing exchange rates from {} ({})", provider.name(),
                snapshot == null ? "no cached rates yet" : "cache older than " + Durations.human(properties.provider().cacheTtl()));
        try {
            ExchangeRateSnapshot fetched = provider.fetchLatestRates(properties.baseCurrency().toUpperCase(Locale.ROOT));
            current = fetched;
            lastFailureAt = null;
            return new Rates(fetched, false);
        } catch (ExchangeRateProviderException ex) {
            lastFailureAt = Instant.now(clock);
            // Only the sanitised message: a provider URL may embed the API key.
            log.warn("Exchange-rate refresh from {} failed: {}", provider.name(), ex.getMessage());
            return fallback();
        }
    }

    private Rates fallback() {
        ExchangeRateSnapshot snapshot = current;
        if (snapshot != null) {
            log.warn("Serving stale exchange rates fetched {} ago", Durations.human(age(snapshot)));
            return new Rates(snapshot, true);
        }
        log.error("No exchange rates available: provider {} unreachable and nothing cached", provider.name());
        throw new ExchangeRateUnavailableException(
                "Exchange rates are temporarily unavailable. Please try again in a few minutes.");
    }

    private Duration age(ExchangeRateSnapshot snapshot) {
        return Duration.between(snapshot.fetchedAt(), Instant.now(clock));
    }

    private boolean isFresh(ExchangeRateSnapshot snapshot) {
        return snapshot.fetchedAt().plus(properties.provider().cacheTtl()).isAfter(Instant.now(clock));
    }

    private boolean inBackoff() {
        Instant failure = lastFailureAt;
        return failure != null && failure.plus(properties.provider().failureBackoff()).isAfter(Instant.now(clock));
    }
}
