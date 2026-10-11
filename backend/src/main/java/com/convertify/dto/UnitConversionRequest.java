package com.convertify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Convert one value between two units of the same category. */
public record UnitConversionRequest(
        @NotBlank(message = "Category is required") String category,
        @NotNull(message = "Value is required") BigDecimal value,
        @NotBlank(message = "Source unit is required") String from,
        @NotBlank(message = "Target unit is required") String to) {
}
