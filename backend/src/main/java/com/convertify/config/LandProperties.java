package com.convertify.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Regional land-measurement definitions loaded from {@code catalog/land-regions.yml}. */
@Validated
@ConfigurationProperties(prefix = "convertify.land")
public record LandProperties(
        @NotBlank String defaultRegion,
        @NotBlank String areaCategory,
        String disclaimer,
        @NotEmpty List<@Valid Region> regions) {

    public record Region(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            List<@Valid UnitDefinitionProperties> units) {

        public Region {
            units = units == null ? List.of() : List.copyOf(units);
        }
    }
}
