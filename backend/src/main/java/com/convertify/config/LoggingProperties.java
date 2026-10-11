package com.convertify.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application-side logging settings. File locations and retention under the same prefix
 * ({@code directory}, {@code max-history}, ...) are read directly by {@code logback-spring.xml}.
 *
 * @param requestIdHeader       header used to accept and return the correlation id
 * @param slowRequestThreshold  API requests slower than this are logged at WARN
 */
@Validated
@ConfigurationProperties(prefix = "convertify.logging")
public record LoggingProperties(
        @NotBlank String requestIdHeader,
        @NotNull Duration slowRequestThreshold) {
}
