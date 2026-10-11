package com.convertify.dto;

import com.convertify.model.DateOperation;
import com.convertify.model.DateUnit;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Outcome of a date shift.
 *
 * @param calendarDays      absolute number of calendar days between start and result
 * @param nonWorkingDaysSkipped weekends/holidays passed over (BUSINESS_DAYS only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DateShiftResponse(
        LocalDate startDate,
        boolean startIsToday,
        DateOperation operation,
        long amount,
        DateUnit unit,
        LocalDate resultDate,
        String resultDayOfWeek,
        long calendarDays,
        Long nonWorkingDaysSkipped,
        CalendarInfoDto calendar,
        String timeZone) {
}
