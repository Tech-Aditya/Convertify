package com.convertify.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The unit catalog loaded from {@code catalog/units.yml}. */
@Validated
@ConfigurationProperties(prefix = "convertify.units")
public record UnitCatalogProperties(
        @NotEmpty List<@Valid Category> categories,
        List<@Valid Preset> presets) {

    public UnitCatalogProperties {
        presets = presets == null ? List.of() : List.copyOf(presets);
    }

    public record Category(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String baseUnit,
            boolean allowNegative,
            String minimumBaseValue,
            @NotEmpty List<@Valid UnitDefinitionProperties> units) {
    }

    public record Preset(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String category,
            @NotEmpty List<String> units,
            @NotBlank String defaultFrom,
            @NotBlank String defaultTo) {
    }
}
