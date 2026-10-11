package com.convertify.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Currency conversion settings and exchange-rate provider connection details. */
@Validated
@ConfigurationProperties(prefix = "convertify.currency")
public record CurrencyProperties(
        @NotBlank String baseCurrency,
        @NotBlank String defaultFrom,
        @NotBlank String defaultTo,
        List<String> featured,
        @Positive int rateScale,
        @NotNull RoundingMode roundingMode,
        @NotNull @Valid Provider provider) {

    public CurrencyProperties {
        featured = featured == null ? List.of() : List.copyOf(featured);
    }

    public record Provider(
            @NotBlank String type,
            @NotBlank String name,
            @NotBlank String url,
            String apiKey,
            String apiKeyHeader,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout,
            @NotNull Duration cacheTtl,
            @NotNull Duration failureBackoff) {

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        /** Never print the API key, even in debug output. */
        @Override
        public String toString() {
            return "Provider[type=%s, name=%s, apiKey=%s]".formatted(type, name, hasApiKey() ? "****" : "<none>");
        }
    }
}
