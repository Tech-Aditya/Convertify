package com.convertify.util;

import com.convertify.config.ConversionProperties;
import com.convertify.exception.InvalidInputException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Business-rule checks on numeric input that depend on configuration (and therefore cannot be
 * expressed as static bean-validation annotations).
 */
@Component
public class InputLimits {

    private final BigDecimal maxAbsoluteValue;
    private final int maxDecimalPlaces;

    public InputLimits(ConversionProperties properties) {
        this.maxAbsoluteValue = properties.maxAbsoluteValue();
        this.maxDecimalPlaces = properties.maxInputDecimalPlaces();
    }

    /**
     * Bounds the magnitude and the number of decimal places. The latter matters as much as the former:
     * JSON such as {@code 1e-999999999} is a short, valid number whose plain form has a billion digits.
     */
    public void requireWithinRange(BigDecimal value, String label) {
        if (value == null) {
            throw new InvalidInputException("%s is required".formatted(label));
        }
        if (value.abs().compareTo(maxAbsoluteValue) > 0) {
            throw new InvalidInputException("%s is too large — the maximum supported magnitude is %s"
                    .formatted(label, maxAbsoluteValue.toPlainString()));
        }
        if (value.signum() != 0 && value.stripTrailingZeros().scale() > maxDecimalPlaces) {
            throw new InvalidInputException("%s has too many decimal places — at most %d are supported"
                    .formatted(label, maxDecimalPlaces));
        }
    }

    public void requireNonNegative(BigDecimal value, String label) {
        requireWithinRange(value, label);
        if (value.signum() < 0) {
            throw new InvalidInputException("%s cannot be negative".formatted(label));
        }
    }

    public void requirePositive(BigDecimal value, String label) {
        requireWithinRange(value, label);
        if (value.signum() <= 0) {
            throw new InvalidInputException("%s must be greater than zero".formatted(label));
        }
    }
}
