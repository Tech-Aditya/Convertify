package com.convertify.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class DecimalExpressionsTest {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);

    @Test
    void parsesPlainNumbers() {
        assertThat(DecimalExpressions.parse("0.3048", MC)).isEqualByComparingTo("0.3048");
    }

    @Test
    void evaluatesMultiplyAndDivideLeftToRight() {
        assertThat(DecimalExpressions.parse("1000/3600", MC))
                .isEqualByComparingTo(new BigDecimal(1000).divide(new BigDecimal(3600), MC));
        assertThat(DecimalExpressions.parse("459.67 * 5 / 9", MC))
                .isEqualByComparingTo(new BigDecimal("459.67").multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(9), MC));
    }

    @Test
    void rejectsMalformedExpressions() {
        assertThatThrownBy(() -> DecimalExpressions.parse(" ", MC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DecimalExpressions.parse("1/", MC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DecimalExpressions.parse("1+2", MC)).isInstanceOf(IllegalArgumentException.class);
    }
}
