package com.convertify.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Breaks a number of seconds into days/hours/minutes/seconds and renders it as readable text. */
public final class DurationFormatter {

    private static final BigInteger SECONDS_PER_MINUTE = BigInteger.valueOf(60);
    private static final BigInteger SECONDS_PER_HOUR = BigInteger.valueOf(3_600);
    private static final BigInteger SECONDS_PER_DAY = BigInteger.valueOf(86_400);

    private DurationFormatter() {
    }

    public record Breakdown(BigInteger days, int hours, int minutes, int seconds) {

        /** "6 hours 15 minutes", "1 day 2 hours", "less than a second". */
        public String humanise() {
            List<String> parts = new ArrayList<>();
            if (days.signum() > 0) {
                parts.add(plural(days.toString(), days.equals(BigInteger.ONE), "day"));
            }
            if (hours > 0) {
                parts.add(plural(String.valueOf(hours), hours == 1, "hour"));
            }
            if (minutes > 0) {
                parts.add(plural(String.valueOf(minutes), minutes == 1, "minute"));
            }
            if (seconds > 0) {
                parts.add(plural(String.valueOf(seconds), seconds == 1, "second"));
            }
            return String.join(" ", parts);
        }

        private static String plural(String amount, boolean singular, String noun) {
            return amount + " " + noun + (singular ? "" : "s");
        }
    }

    /** Rounds to the nearest whole second. */
    public static Breakdown breakdown(BigDecimal totalSeconds) {
        BigInteger seconds = totalSeconds.setScale(0, RoundingMode.HALF_UP).toBigIntegerExact();
        BigInteger[] days = seconds.divideAndRemainder(SECONDS_PER_DAY);
        BigInteger[] hours = days[1].divideAndRemainder(SECONDS_PER_HOUR);
        BigInteger[] minutes = hours[1].divideAndRemainder(SECONDS_PER_MINUTE);
        return new Breakdown(days[0], hours[0].intValue(), minutes[0].intValue(), minutes[1].intValue());
    }

    public static String humanise(BigDecimal totalSeconds) {
        if (totalSeconds.signum() == 0) {
            return "0 seconds";
        }
        String text = breakdown(totalSeconds).humanise();
        return text.isEmpty() ? "less than a second" : text;
    }
}
