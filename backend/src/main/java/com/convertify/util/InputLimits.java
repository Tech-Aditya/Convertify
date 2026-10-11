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

    public InputLimits(ConversionProperties properties) {
        this.maxAbsoluteValue = properties.maxAbsoluteValue();
    }

    public void requireWithinRange(BigDecimal value, String label) {
        if (value == null) {
            throw new InvalidInputException("%s is required".formatted(label));
        }
        if (value.abs().compareTo(maxAbsoluteValue) > 0) {
            throw new InvalidInputException("%s is too large — the maximum supported magnitude is %s"
                    .formatted(label, maxAbsoluteValue.toPlainString()));
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
