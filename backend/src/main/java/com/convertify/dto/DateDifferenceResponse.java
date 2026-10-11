package com.convertify.dto;

import java.time.LocalDate;

/**
 * Difference between two dates in several useful forms.
 *
 * @param weeks/remainingDays  total days as whole weeks plus days
 * @param years/months/days    calendar period, e.g. 2 months 19 days
 * @param businessDays         working days in the range under the applied calendar
 */
public record DateDifferenceResponse(
        LocalDate startDate,
        LocalDate endDate,
        boolean includeEndDate,
        long totalDays,
        long weeks,
        long remainingDays,
        int years,
        int months,
        int days,
        long businessDays,
        CalendarInfoDto calendar) {
}
