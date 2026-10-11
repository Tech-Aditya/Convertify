package com.convertify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Convert an amount of money between two ISO-4217 currencies. */
public record CurrencyConversionRequest(
        @NotNull(message = "Amount is required")
        @PositiveOrZero(message = "Amount cannot be negative")
        BigDecimal amount,

        @NotBlank(message = "Source currency is required")
        @Pattern(regexp = "[A-Za-z]{3}", message = "Source currency must be a 3-letter code")
        String from,

        @NotBlank(message = "Target currency is required")
        @Pattern(regexp = "[A-Za-z]{3}", message = "Target currency must be a 3-letter code")
        String to) {
}
