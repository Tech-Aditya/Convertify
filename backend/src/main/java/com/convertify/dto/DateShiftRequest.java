package com.convertify.dto;

import com.convertify.model.DateOperation;
import com.convertify.model.DateUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Add or subtract an amount of time from a date.
 *
 * @param date            start date; omit to use today in the configured time zone
 * @param weekendPreset   BUSINESS_DAYS only; defaults to the configured preset
 * @param holidayCalendar BUSINESS_DAYS only; omit for no holidays
 */
public record DateShiftRequest(
        LocalDate date,
        @NotNull(message = "Operation is required (ADD or SUBTRACT)") DateOperation operation,
        @NotNull(message = "Amount is required") @PositiveOrZero(message = "Amount cannot be negative") Long amount,
        @NotNull(message = "Unit is required") DateUnit unit,
        String weekendPreset,
        String holidayCalendar) {
}
