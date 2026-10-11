package com.convertify.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * A unit expressed relative to its category's base unit:
 * {@code base = value * factor + offset}.
 *
 * @param basis optional human-readable definition, used for region-dependent land units
 */
public record UnitDefinition(
        String code,
        String name,
        String symbol,
        BigDecimal factor,
        BigDecimal offset,
        String basis) {

    public UnitDefinition {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(factor, "factor");
        offset = offset == null ? BigDecimal.ZERO : offset;
        if (factor.signum() <= 0) {
            throw new IllegalArgumentException("Unit '%s' must have a positive factor".formatted(code));
        }
    }

    public BigDecimal toBase(BigDecimal value, MathContext mc) {
        return value.multiply(factor, mc).add(offset, mc);
    }

    public BigDecimal fromBase(BigDecimal baseValue, MathContext mc) {
        return baseValue.subtract(offset, mc).divide(factor, mc);
    }

    public boolean isRegional() {
        return basis != null && !basis.isBlank();
    }
}
