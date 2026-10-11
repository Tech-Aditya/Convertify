package com.convertify.model;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/** A named set of holidays: dates recurring every year plus one-off dates. */
public record HolidayCalendar(String code, String name, String description, Set<MonthDay> recurring,
                              Set<LocalDate> dates) {

    public static final HolidayCalendar NONE = new HolidayCalendar("NONE", "No holidays", null, Set.of(), Set.of());

    public HolidayCalendar {
        recurring = Set.copyOf(recurring);
        dates = Set.copyOf(dates);
    }

    public boolean isHoliday(LocalDate date) {
        return dates.contains(date) || recurring.contains(MonthDay.from(date));
    }
}
