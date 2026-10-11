package com.convertify.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Branding and version information exposed to the frontend. */
@Validated
@ConfigurationProperties(prefix = "convertify.app")
public record AppProperties(
        @NotBlank String name,
        @NotBlank String tagline,
        String description,
        String version) {
}
