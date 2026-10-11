package com.convertify.service;

import com.convertify.config.DateProperties;
import com.convertify.dto.CalendarInfoDto;
import com.convertify.dto.DateDifferenceRequest;
import com.convertify.dto.DateDifferenceResponse;
import com.convertify.dto.DateOptionsResponse;
import com.convertify.dto.DateShiftRequest;
import com.convertify.dto.DateShiftResponse;
import com.convertify.exception.ErrorCode;
import com.convertify.exception.InvalidInputException;
import com.convertify.model.DateUnit;
import com.convertify.model.WorkingCalendar;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Date arithmetic: shifting by days/weeks/months/years/business days and measuring differences. */
@Service
public class DateCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(DateCalculatorService.class);

    private final DateProperties properties;
    private final WorkingCalendarService calendars;
    private final Clock clock;

    public DateCalculatorService(DateProperties properties, WorkingCalendarService calendars, Clock clock) {
        this.properties = properties;
        this.calendars = calendars;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(properties.defaultTimeZone()));
    }

    public DateOptionsResponse options() {
        return new DateOptionsResponse(
                today(),
                properties.defaultTimeZone().getId(),
                properties.maxShiftAmount(),
                calendars.defaultWeekendPreset(),
                calendars.weekendPresets().stream()
                        .map(p -> new DateOptionsResponse.WeekendPreset(p.code(), p.label(), p.days()))
                        .toList(),
                calendars.holidayCalendars().stream()
                        .map(c -> new DateOptionsResponse.Calendar(c.code(), c.name(), c.description()))
                        .toList());
    }

    public DateShiftResponse shift(DateShiftRequest request) {
        LocalDate today = today();
        LocalDate start = request.date() != null ? request.date() : today;
        requireSupportedYear(start, "Start date");
        if (request.amount() > properties.maxShiftAmount()) {
            throw new InvalidInputException("Amount cannot exceed %,d".formatted(properties.maxShiftAmount()));
        }
        long signedAmount = request.operation().apply(request.amount());
        log.info("Date shift requested: {} {} {} from {}", request.operation(), request.amount(), request.unit(),
                request.date() == null ? "today" : "a given date");

        LocalDate result;
        Long skipped = null;
        CalendarInfoDto calendarInfo = null;
        try {
            if (request.unit() == DateUnit.BUSINESS_DAYS) {
                WorkingCalendar calendar = calendars.resolve(request.weekendPreset(), request.holidayCalendar());
                result = addBusinessDays(start, signedAmount, calendar);
                skipped = Math.abs(ChronoUnit.DAYS.between(start, result)) - request.amount();
                calendarInfo = info(calendar);
            } else {
                result = switch (request.unit()) {
                    case DAYS -> start.plusDays(signedAmount);
                    case WEEKS -> start.plusWeeks(signedAmount);
                    case MONTHS -> start.plusMonths(signedAmount);
                    case YEARS -> start.plusYears(signedAmount);
                    case BUSINESS_DAYS -> throw new IllegalStateException("handled above");
                };
            }
        } catch (DateTimeException | ArithmeticException ex) {
            throw outOfRange();
        }
        requireSupportedYear(result, "The resulting date");

        return new DateShiftResponse(
                start,
                start.equals(today),
                request.operation(),
                request.amount(),
                request.unit(),
                result,
                result.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                Math.abs(ChronoUnit.DAYS.between(start, result)),
                skipped,
                calendarInfo,
                properties.defaultTimeZone().getId());
    }

    public DateDifferenceResponse difference(DateDifferenceRequest request) {
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        requireSupportedYear(start, "Start date");
        requireSupportedYear(end, "End date");
        if (end.isBefore(start)) {
            throw new InvalidInputException(ErrorCode.INVALID_DATE_RANGE, "End date must be on or after the start date");
        }
        LocalDate exclusiveEnd = request.includeEndDate() ? end.plusDays(1) : end;
        long totalDays = ChronoUnit.DAYS.between(start, exclusiveEnd);
        if (totalDays > properties.maxRangeDays()) {
            throw new InvalidInputException(ErrorCode.INVALID_DATE_RANGE,
                    "Date range is too long — the maximum is %,d days".formatted(properties.maxRangeDays()));
        }
        log.info("Date difference requested: {} days (end date {})", totalDays,
                request.includeEndDate() ? "included" : "excluded");

        WorkingCalendar calendar = calendars.resolve(request.weekendPreset(), request.holidayCalendar());
        Period period = Period.between(start, exclusiveEnd);
        return new DateDifferenceResponse(
                start,
                end,
                request.includeEndDate(),
                totalDays,
                totalDays / 7,
                totalDays % 7,
                period.getYears(),
                period.getMonths(),
                period.getDays(),
                countBusinessDays(start, exclusiveEnd, calendar),
                info(calendar));
    }

    /**
     * Moves {@code amount} business days away from {@code start}; the start date itself is not counted.
     * Negative amounts move backwards.
     */
    private LocalDate addBusinessDays(LocalDate start, long amount, WorkingCalendar calendar) {
        long step = amount >= 0 ? 1 : -1;
        long remaining = Math.abs(amount);
        LocalDate date = start;
        while (remaining > 0) {
            date = date.plusDays(step);
            if (date.getYear() < properties.minYear() || date.getYear() > properties.maxYear()) {
                throw outOfRange();
            }
            if (calendar.isBusinessDay(date)) {
                remaining--;
            }
        }
        return date;
    }

    /** Business days in [start, exclusiveEnd). */
    private static long countBusinessDays(LocalDate start, LocalDate exclusiveEnd, WorkingCalendar calendar) {
        return start.datesUntil(exclusiveEnd).filter(calendar::isBusinessDay).count();
    }

    private void requireSupportedYear(LocalDate date, String label) {
        if (date.getYear() < properties.minYear() || date.getYear() > properties.maxYear()) {
            throw new InvalidInputException("%s must be between year %d and %d"
                    .formatted(label, properties.minYear(), properties.maxYear()));
        }
    }

    private InvalidInputException outOfRange() {
        return new InvalidInputException("The resulting date is outside the supported range (years %d–%d)"
                .formatted(properties.minYear(), properties.maxYear()));
    }

    private static CalendarInfoDto info(WorkingCalendar calendar) {
        return new CalendarInfoDto(calendar.weekendLabel(), calendar.holidays().name());
    }
}
