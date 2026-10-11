package com.convertify.provider.currency;

import com.convertify.config.CurrencyProperties;
import com.convertify.exception.ExchangeRateProviderException;
import com.convertify.model.ExchangeRateSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP provider compatible with the ExchangeRate-API response format
 * ({@code open.er-api.com} and {@code v6.exchangerate-api.com}). The URL template, API key and
 * key header all come from configuration.
 */
@Component
@ConditionalOnProperty(prefix = "convertify.currency.provider", name = "type", havingValue = "external",
        matchIfMissing = true)
public class ExternalExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalExchangeRateProvider.class);
    private static final String SUCCESS = "success";

    private final CurrencyProperties.Provider config;
    private final RestClient restClient;
    private final Clock clock;

    public ExternalExchangeRateProvider(CurrencyProperties properties, Clock clock) {
        this.config = properties.provider();
        this.clock = clock;
        // HttpURLConnection-based: no background selector thread, which suits one call every few hours.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(config.connectTimeout());
        requestFactory.setReadTimeout(config.readTimeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        log.info("Exchange-rate provider configured: {}", config);
    }

    @Override
    public ExchangeRateSnapshot fetchLatestRates(String baseCurrency) {
        long start = System.nanoTime();
        log.debug("Calling exchange-rate provider {} for base {}", config.name(), baseCurrency);
        ProviderResponse response;
        try {
            response = restClient.get()
                    .uri(config.url(), Map.of("base", baseCurrency, "apiKey", config.hasApiKey() ? config.apiKey() : ""))
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (config.hasApiKey() && config.apiKeyHeader() != null && !config.apiKeyHeader().isBlank()) {
                            headers.set(config.apiKeyHeader(), config.apiKey());
                        }
                    })
                    .retrieve()
                    .body(ProviderResponse.class);
        } catch (RestClientException ex) {
            // Exception messages can contain the request URL (and so an API key): report types only.
            Throwable root = ex.getMostSpecificCause();
            throw new ExchangeRateProviderException("Request to %s failed (%s)".formatted(config.name(),
                    root.getClass().getSimpleName()));
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        if (response == null) {
            throw new ExchangeRateProviderException("Empty response from " + config.name());
        }
        if (response.result() != null && !SUCCESS.equalsIgnoreCase(response.result())) {
            throw new ExchangeRateProviderException("%s returned an error: %s".formatted(config.name(), response.errorType()));
        }
        Map<String, BigDecimal> rawRates = response.rates() != null ? response.rates() : response.conversionRates();
        if (rawRates == null || rawRates.isEmpty()) {
            throw new ExchangeRateProviderException("%s returned no rates".formatted(config.name()));
        }

        Map<String, BigDecimal> rates = new HashMap<>();
        rawRates.forEach((code, rate) -> {
            if (code != null && rate != null && rate.signum() > 0) {
                rates.put(code.toUpperCase(Locale.ROOT), rate);
            }
        });
        String base = response.baseCode() != null ? response.baseCode().toUpperCase(Locale.ROOT) : baseCurrency;
        rates.putIfAbsent(base, BigDecimal.ONE);

        log.info("Fetched {} exchange rates from {} (base {}) in {} ms", rates.size(), config.name(), base, elapsedMs);
        return new ExchangeRateSnapshot(base, rates, epoch(response.lastUpdateUnix()), epoch(response.nextUpdateUnix()),
                Instant.now(clock), config.name());
    }

    @Override
    public String name() {
        return config.name();
    }

    private static Instant epoch(Long seconds) {
        return seconds == null || seconds <= 0 ? null : Instant.ofEpochSecond(seconds);
    }

    /** Wire format. {@code rates} (open API) and {@code conversion_rates} (keyed API) are both accepted. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProviderResponse(
            String result,
            @JsonProperty("error-type") String errorType,
            @JsonProperty("base_code") String baseCode,
            Map<String, BigDecimal> rates,
            @JsonProperty("conversion_rates") Map<String, BigDecimal> conversionRates,
            @JsonProperty("time_last_update_unix") Long lastUpdateUnix,
            @JsonProperty("time_next_update_unix") Long nextUpdateUnix) {
    }
}
