package com.convertify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Convert an area within a region. {@code to} may be omitted when converting to all units. */
public record LandConversionRequest(
        @NotBlank(message = "Region is required") String region,
        @NotNull(message = "Value is required") BigDecimal value,
        @NotBlank(message = "Source unit is required") String from,
        String to) {
}
