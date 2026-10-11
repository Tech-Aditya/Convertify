package com.convertify.config;

import jakarta.validation.constraints.NotBlank;

/**
 * Raw unit definition as written in the catalog files. {@code factor} and {@code offset}
 * are exact decimal expressions and are parsed once at startup.
 */
public record UnitDefinitionProperties(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String symbol,
        @NotBlank String factor,
        String offset,
        String basis) {
}
