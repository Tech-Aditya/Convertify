package com.convertify.util;

import com.convertify.config.ConversionProperties;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/**
 * Rounds full-precision results into clean display values: {@code 1.8} rather than
 * {@code 1.8000000000000003}, and {@code 0.000000621371} rather than {@code 0}.
 */
@Component
public class NumberDisplayFormatter {

    private final ConversionProperties properties;
    private final MathContext significantContext;

    public NumberDisplayFormatter(ConversionProperties properties) {
        this.properties = properties;
        this.significantContext = new MathContext(properties.significantDigits(), properties.roundingMode());
    }

    /** Magnitudes >= 1 keep a fixed number of decimals; smaller magnitudes keep significant digits. */
    public BigDecimal round(BigDecimal value) {
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rounded = value.abs().compareTo(BigDecimal.ONE) >= 0
                ? value.setScale(properties.maxDecimalPlaces(), properties.roundingMode())
                : value.round(significantContext);
        return normalise(rounded);
    }

    /** Plain (never scientific) string form of {@link #round(BigDecimal)}. */
    public String format(BigDecimal value) {
        return round(value).toPlainString();
    }

    /** Strips trailing zeros without switching to exponent notation for whole numbers. */
    public static BigDecimal normalise(BigDecimal value) {
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
