package com.convertify.model;

import java.math.BigDecimal;

/**
 * Full-precision outcome of a unit conversion. Rounding for display happens later.
 *
 * @param unitRate the value of one {@code from} unit expressed in {@code to} units
 */
public record ConversionResult(
        UnitCategory category,
        BigDecimal value,
        UnitDefinition from,
        BigDecimal result,
        UnitDefinition to,
        BigDecimal unitRate) {
}
