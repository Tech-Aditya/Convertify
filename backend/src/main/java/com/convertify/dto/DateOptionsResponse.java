package com.convertify.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Everything the date calculator form needs: today, limits and business-day options. */
public record DateOptionsResponse(
        LocalDate today,
        String timeZone,
        long maxShiftAmount,
        String defaultWeekendPreset,
        List<WeekendPreset> weekendPresets,
        List<Calendar> holidayCalendars) {

    public record WeekendPreset(String code, String label, Set<DayOfWeek> days) {
    }

    public record Calendar(String code, String name, String description) {
    }
}
