package com.convertify.util;

import java.time.Duration;
import java.util.Locale;

/** Compact, human-friendly durations for log messages: {@code PT1H30M} → {@code 1h30m}. */
public final class Durations {

    private Durations() {
    }

    public static String human(Duration duration) {
        if (duration == null) {
            return "n/a";
        }
        Duration whole = duration.withNanos(0);
        return whole.isZero() ? "0s" : whole.toString().substring(2).toLowerCase(Locale.ROOT);
    }
}
