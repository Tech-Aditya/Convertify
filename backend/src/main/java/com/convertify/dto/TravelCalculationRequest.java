package com.convertify.dto;

import com.convertify.model.TravelQuantity;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Solve Distance = Speed × Time for one quantity. Supply the other two values with their units;
 * the unit of the solved quantity (e.g. {@code distanceUnit} when solving for DISTANCE) selects the
 * unit of the result. When solving for TIME the result unit is optional (defaults to hours).
 */
public record TravelCalculationRequest(
        @NotNull(message = "Choose what to calculate: DISTANCE, SPEED or TIME") TravelQuantity solveFor,
        BigDecimal distance,
        String distanceUnit,
        BigDecimal speed,
        String speedUnit,
        BigDecimal time,
        String timeUnit) {
}
