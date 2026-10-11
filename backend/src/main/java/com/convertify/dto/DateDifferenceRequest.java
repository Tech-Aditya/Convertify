package com.convertify.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Difference between two dates.
 *
 * @param includeEndDate count the end date itself (e.g. for "how many days is this booking")
 */
public record DateDifferenceRequest(
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        Boolean includeEndDate,
        String weekendPreset,
        String holidayCalendar) {

    public DateDifferenceRequest {
        includeEndDate = Boolean.TRUE.equals(includeEndDate);
    }
}
