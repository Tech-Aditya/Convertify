package com.convertify.dto;

import com.convertify.model.TravelQuantity;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Result of a time/distance/speed calculation.
 *
 * @param readable human-friendly form, e.g. "6 hours 15 minutes" (TIME only)
 * @param duration whole-second breakdown (TIME only)
 * @param inputs   the two known quantities, echoed back
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TravelCalculationResponse(
        TravelQuantity solveFor,
        String formula,
        QuantityDto result,
        String readable,
        Duration duration,
        List<QuantityDto> inputs) {

    public record Duration(String days, int hours, int minutes, int seconds) {
    }
}
