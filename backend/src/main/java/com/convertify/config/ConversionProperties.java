package com.convertify.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Numeric precision, display rounding and input limits shared by every calculator. */
@Validated
@ConfigurationProperties(prefix = "convertify.conversion")
public record ConversionProperties(
        @Positive int calculationPrecision,
        @Positive int maxDecimalPlaces,
        @Positive int significantDigits,
        @NotNull RoundingMode roundingMode,
        @NotNull @Positive BigDecimal maxAbsoluteValue,
        @Positive int maxInputDecimalPlaces) {

    /** Context used for all intermediate arithmetic. */
    public MathContext mathContext() {
        return new MathContext(calculationPrecision, RoundingMode.HALF_EVEN);
    }
}
