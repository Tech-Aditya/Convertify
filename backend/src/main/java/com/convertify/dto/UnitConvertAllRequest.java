package com.convertify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Convert one value into many units at once. When {@code units} is empty, all units of the category are used. */
public record UnitConvertAllRequest(
        @NotBlank(message = "Category is required") String category,
        @NotNull(message = "Value is required") BigDecimal value,
        @NotBlank(message = "Source unit is required") String from,
        @Size(max = 50, message = "At most 50 target units can be requested") List<String> units) {
}
