package com.convertify.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Which unit categories the time/distance/speed calculator draws from, and the coherent set of
 * units (distance / time = speed) every input is normalised to before calculating.
 */
@Validated
@ConfigurationProperties(prefix = "convertify.travel")
public record TravelProperties(
        @NotBlank String distanceCategory,
        @NotBlank String speedCategory,
        @NotBlank String timeCategory,
        @NotBlank String normalizedDistanceUnit,
        @NotBlank String normalizedSpeedUnit,
        @NotBlank String normalizedTimeUnit,
        @NotBlank String defaultTimeResultUnit,
        @NotBlank String defaultDistanceUnit,
        @NotBlank String defaultSpeedUnit) {
}
