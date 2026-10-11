package com.convertify.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.convertify.config.ConversionProperties;
import com.convertify.exception.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class NumberFormattingTest {

    private static final ConversionProperties PROPERTIES =
            new ConversionProperties(34, 6, 6, RoundingMode.HALF_UP, new BigDecimal("1000000000000000"), 20);

    private final NumberDisplayFormatter formatter = new NumberDisplayFormatter(PROPERTIES);
    private final InputLimits limits = new InputLimits(PROPERTIES);

    @Test
    void roundsAwayFloatingPointNoise() {
        assertThat(formatter.format(new BigDecimal("1.8000000000000003"))).isEqualTo("1.8");
        assertThat(formatter.format(new BigDecimal("1200.0000"))).isEqualTo("1200");
    }

    @Test
    void keepsSignificantDigitsForSmallMagnitudes() {
        assertThat(formatter.format(new BigDecimal("0.000000621371192237"))).isEqualTo("0.000000621371");
        assertThat(formatter.format(BigDecimal.ZERO.setScale(10))).isEqualTo("0");
    }

    @Test
    void acceptsOrdinaryInput() {
        assertThatCode(() -> limits.requireWithinRange(new BigDecimal("-273.15"), "Value")).doesNotThrowAnyException();
        assertThatCode(() -> limits.requireWithinRange(new BigDecimal("1.50000000000000000000000000"), "Value"))
                .as("trailing zeros do not count as decimal places")
                .doesNotThrowAnyException();
        assertThatCode(() -> limits.requireWithinRange(new BigDecimal("0E-999999999"), "Value")).doesNotThrowAnyException();
    }

    @Test
    void rejectsValuesOutsideTheSupportedRange() {
        assertThatThrownBy(() -> limits.requireWithinRange(new BigDecimal("1E16"), "Value"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("too large");
        assertThatThrownBy(() -> limits.requireWithinRange(new BigDecimal("1E-999999999"), "Value"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("too many decimal places");
        assertThatThrownBy(() -> limits.requirePositive(BigDecimal.ZERO, "Speed"))
                .hasMessage("Speed must be greater than zero");
        assertThatThrownBy(() -> limits.requireNonNegative(BigDecimal.ONE.negate(), "Amount"))
                .hasMessage("Amount cannot be negative");
    }

    @Test
    void humanisesDurations() {
        assertThat(DurationFormatter.humanise(new BigDecimal("22500"))).isEqualTo("6 hours 15 minutes");
        assertThat(DurationFormatter.humanise(new BigDecimal("93661"))).isEqualTo("1 day 2 hours 1 minute 1 second");
        assertThat(DurationFormatter.humanise(new BigDecimal("0.2"))).isEqualTo("less than a second");
        assertThat(DurationFormatter.humanise(BigDecimal.ZERO)).isEqualTo("0 seconds");
    }
}
