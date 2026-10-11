package com.convertify.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.regex.Pattern;

/**
 * Parses the small exact-decimal expressions used in the catalogs, e.g. {@code "0.3048"},
 * {@code "1000/3600"} or {@code "459.67*5/9"}. Only {@code *} and {@code /} are supported and are
 * evaluated left to right, which keeps factors exact and readable without a general expression engine.
 */
public final class DecimalExpressions {

    private static final Pattern TOKENS = Pattern.compile("(?=[*/])|(?<=[*/])");

    private DecimalExpressions() {
    }

    public static BigDecimal parse(String expression, MathContext mc) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression must not be blank");
        }
        String[] tokens = TOKENS.split(expression.replace(" ", ""));
        BigDecimal result = number(tokens[0], expression);
        for (int i = 1; i < tokens.length; i += 2) {
            if (i + 1 >= tokens.length) {
                throw new IllegalArgumentException("Dangling operator in expression '%s'".formatted(expression));
            }
            BigDecimal operand = number(tokens[i + 1], expression);
            result = switch (tokens[i]) {
                case "*" -> result.multiply(operand, mc);
                case "/" -> result.divide(operand, mc);
                default -> throw new IllegalArgumentException("Unsupported operator in '%s'".formatted(expression));
            };
        }
        return result;
    }

    private static BigDecimal number(String token, String expression) {
        try {
            return new BigDecimal(token);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number '%s' in expression '%s'".formatted(token, expression), ex);
        }
    }
}
