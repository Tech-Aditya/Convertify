package com.convertify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.convertify.config.CurrencyProperties;
import com.convertify.exception.ExchangeRateProviderException;
import com.convertify.exception.ExchangeRateUnavailableException;
import com.convertify.model.ExchangeRateSnapshot;
import com.convertify.provider.currency.ExchangeRateProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExchangeRateCacheTest {

    private static final Duration TTL = Duration.ofHours(1);
    private static final Duration BACKOFF = Duration.ofMinutes(5);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-01T00:00:00Z"));
    private final FakeProvider provider = new FakeProvider();
    private final ExchangeRateCache cache = new ExchangeRateCache(provider, properties(), clock);

    @Test
    void reusesRatesWithinTtl() {
        cache.get();
        clock.advance(TTL.minusMinutes(1));
        ExchangeRateCache.Rates rates = cache.get();

        assertThat(provider.calls).isEqualTo(1);
        assertThat(rates.stale()).isFalse();
    }

    @Test
    void refreshesOnceTtlExpires() {
        cache.get();
        clock.advance(TTL.plusSeconds(1));
        cache.get();

        assertThat(provider.calls).isEqualTo(2);
    }

    @Test
    void servesStaleRatesAndBacksOffWhenRefreshFails() {
        cache.get();
        clock.advance(TTL.plusSeconds(1));
        provider.failing = true;

        assertThat(cache.get().stale()).isTrue();
        assertThat(cache.get().stale()).isTrue();
        assertThat(provider.calls).as("no retry during back-off").isEqualTo(2);

        clock.advance(BACKOFF.plusSeconds(1));
        provider.failing = false;
        assertThat(cache.get().stale()).isFalse();
        assertThat(provider.calls).isEqualTo(3);
    }

    @Test
    void failsWhenNothingIsCached() {
        provider.failing = true;

        assertThatThrownBy(cache::get).isInstanceOf(ExchangeRateUnavailableException.class);
    }

    private static CurrencyProperties properties() {
        return new CurrencyProperties("USD", "USD", "INR", List.of(), 6, RoundingMode.HALF_EVEN,
                new CurrencyProperties.Provider("fake", "Fake", "http://unused", null, null,
                        Duration.ofSeconds(1), Duration.ofSeconds(1), TTL, BACKOFF));
    }

    private final class FakeProvider implements ExchangeRateProvider {

        int calls;
        boolean failing;

        @Override
        public ExchangeRateSnapshot fetchLatestRates(String baseCurrency) {
            calls++;
            if (failing) {
                throw new ExchangeRateProviderException("provider down");
            }
            return new ExchangeRateSnapshot(baseCurrency, Map.of("USD", BigDecimal.ONE), null, null,
                    clock.instant(), name());
        }

        @Override
        public String name() {
            return "Fake";
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
