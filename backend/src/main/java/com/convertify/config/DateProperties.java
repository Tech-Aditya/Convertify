package com.convertify.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Date calculator settings, weekend presets and holiday calendars. */
@Validated
@ConfigurationProperties(prefix = "convertify.date")
public record DateProperties(
        @NotNull ZoneId defaultTimeZone,
        @Positive long maxShiftAmount,
        @Positive long maxRangeDays,
        int minYear,
        int maxYear,
        @NotBlank String defaultWeekendPreset,
        @NotEmpty List<@Valid WeekendPreset> weekendPresets,
        List<@Valid HolidayCalendar> holidayCalendars) {

    public DateProperties {
        holidayCalendars = holidayCalendars == null ? List.of() : List.copyOf(holidayCalendars);
    }

    public record WeekendPreset(@NotBlank String code, @NotBlank String label, Set<DayOfWeek> days) {

        public WeekendPreset {
            // EnumSet keeps week order (Monday → Sunday) for display.
            days = days == null || days.isEmpty()
                    ? Set.of()
                    : Collections.unmodifiableSet(EnumSet.copyOf(days));
        }
    }

    public record HolidayCalendar(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            List<String> recurring,
            List<String> dates) {

        public HolidayCalendar {
            recurring = recurring == null ? List.of() : List.copyOf(recurring);
            dates = dates == null ? List.of() : List.copyOf(dates);
        }
    }
}
