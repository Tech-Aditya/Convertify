package com.convertify.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Weekend days plus a holiday calendar — together they define what a business day is. */
public record WorkingCalendar(String weekendLabel, Set<DayOfWeek> weekendDays, HolidayCalendar holidays) {

    public boolean isBusinessDay(LocalDate date) {
        return !weekendDays.contains(date.getDayOfWeek()) && !holidays.isHoliday(date);
    }
}
