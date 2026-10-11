package com.convertify.service;

import com.convertify.config.DateProperties;
import com.convertify.exception.InvalidInputException;
import com.convertify.model.HolidayCalendar;
import com.convertify.model.WorkingCalendar;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Resolves weekend presets and holiday calendars (from configuration) into {@link WorkingCalendar}s. */
@Service
public class WorkingCalendarService {

    private static final Logger log = LoggerFactory.getLogger(WorkingCalendarService.class);
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

    private final Map<String, DateProperties.WeekendPreset> weekendPresets;
    private final Map<String, HolidayCalendar> holidayCalendars;
    private final DateProperties.WeekendPreset defaultWeekend;

    public WorkingCalendarService(DateProperties properties) {
        this.weekendPresets = properties.weekendPresets().stream().collect(Collectors.toMap(
                preset -> key(preset.code()), preset -> preset, (a, b) -> {
                    throw new IllegalStateException("Duplicate weekend preset " + a.code());
                }, LinkedHashMap::new));
        weekendPresets.values().forEach(preset -> {
            if (preset.days().size() >= 7) {
                throw new IllegalStateException("Weekend preset %s leaves no working days".formatted(preset.code()));
            }
        });
        this.defaultWeekend = weekendPresets.get(key(properties.defaultWeekendPreset()));
        if (defaultWeekend == null) {
            throw new IllegalStateException("Default weekend preset '%s' is not defined"
                    .formatted(properties.defaultWeekendPreset()));
        }

        Map<String, HolidayCalendar> calendars = new LinkedHashMap<>();
        for (DateProperties.HolidayCalendar source : properties.holidayCalendars()) {
            calendars.put(key(source.code()), parse(source));
        }
        this.holidayCalendars = Collections.unmodifiableMap(calendars);
        log.info("Working calendars loaded: {} weekend presets, {} holiday calendars",
                weekendPresets.size(), holidayCalendars.size());
    }

    public WorkingCalendar resolve(String weekendPresetCode, String holidayCalendarCode) {
        DateProperties.WeekendPreset weekend = weekendPresetCode == null || weekendPresetCode.isBlank()
                ? defaultWeekend
                : weekendPresets.get(key(weekendPresetCode));
        if (weekend == null) {
            throw new InvalidInputException("Weekend option '%s' is not supported".formatted(weekendPresetCode));
        }
        HolidayCalendar holidays = HolidayCalendar.NONE;
        if (holidayCalendarCode != null && !holidayCalendarCode.isBlank()
                && !HolidayCalendar.NONE.code().equalsIgnoreCase(holidayCalendarCode)) {
            holidays = holidayCalendars.get(key(holidayCalendarCode));
            if (holidays == null) {
                throw new InvalidInputException("Holiday calendar '%s' is not supported".formatted(holidayCalendarCode));
            }
        }
        return new WorkingCalendar(weekend.label(), weekend.days(), holidays);
    }

    public List<DateProperties.WeekendPreset> weekendPresets() {
        return List.copyOf(weekendPresets.values());
    }

    public List<HolidayCalendar> holidayCalendars() {
        return List.copyOf(holidayCalendars.values());
    }

    public String defaultWeekendPreset() {
        return defaultWeekend.code();
    }

    private static HolidayCalendar parse(DateProperties.HolidayCalendar source) {
        try {
            return new HolidayCalendar(
                    source.code(),
                    source.name(),
                    source.description(),
                    source.recurring().stream().map(value -> MonthDay.parse(value, MONTH_DAY))
                            .collect(Collectors.toSet()),
                    source.dates().stream().map(LocalDate::parse).collect(Collectors.toSet()));
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Invalid date in holiday calendar %s: %s"
                    .formatted(source.code(), ex.getParsedString()), ex);
        }
    }

    private static String key(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
