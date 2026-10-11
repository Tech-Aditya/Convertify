package com.convertify.converter;

import com.convertify.exception.InvalidInputException;
import com.convertify.model.ConversionResult;
import com.convertify.model.UnitCategory;
import com.convertify.model.UnitDefinition;
import com.convertify.util.InputLimits;
import com.convertify.util.NumberDisplayFormatter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The one conversion algorithm used by every calculator:
 * input → normalise to the category's base unit → de-normalise to the target unit.
 */
@Component
public class UnitConversionEngine {

    private final UnitRegistry registry;
    private final InputLimits inputLimits;
    private final NumberDisplayFormatter formatter;
    private final MathContext mathContext;

    public UnitConversionEngine(UnitRegistry registry, InputLimits inputLimits, NumberDisplayFormatter formatter,
                                UnitDefinitionFactory factory) {
        this.registry = registry;
        this.inputLimits = inputLimits;
        this.formatter = formatter;
        this.mathContext = factory.mathContext();
    }

    public ConversionResult convert(UnitCategory category, BigDecimal value, String fromCode, String toCode) {
        UnitDefinition from = registry.unit(category, fromCode);
        UnitDefinition to = registry.unit(category, toCode);
        validate(category, value, from);
        return result(category, value, from, to);
    }

    /** Converts {@code value} into each of {@code targetCodes} (or every unit when empty), skipping the source. */
    public List<ConversionResult> convertAll(UnitCategory category, BigDecimal value, String fromCode,
                                             List<String> targetCodes) {
        UnitDefinition from = registry.unit(category, fromCode);
        validate(category, value, from);
        List<UnitDefinition> targets = targetCodes == null || targetCodes.isEmpty()
                ? category.units()
                : targetCodes.stream().map(code -> registry.unit(category, code)).distinct().toList();
        return targets.stream()
                .filter(target -> !target.equals(from))
                .map(target -> result(category, value, from, target))
                .toList();
    }

    /** Converts without the user-input checks; for internal normalisation of already-validated values. */
    public BigDecimal convertValue(BigDecimal value, UnitDefinition from, UnitDefinition to) {
        if (from.equals(to)) {
            return value;
        }
        return to.fromBase(from.toBase(value, mathContext), mathContext);
    }

    private ConversionResult result(UnitCategory category, BigDecimal value, UnitDefinition from, UnitDefinition to) {
        return new ConversionResult(category, value, from, convertValue(value, from, to), to,
                unitRate(from, to));
    }

    /** "1 from = x to" — offset-aware, so 1 °C = 33.8 °F. */
    private BigDecimal unitRate(UnitDefinition from, UnitDefinition to) {
        return convertValue(BigDecimal.ONE, from, to);
    }

    private void validate(UnitCategory category, BigDecimal value, UnitDefinition from) {
        String label = "Value";
        if (category.allowNegative()) {
            inputLimits.requireWithinRange(value, label);
        } else {
            inputLimits.requireNonNegative(value, label);
        }
        category.minimumBaseValue().ifPresent(minimum -> {
            if (from.toBase(value, mathContext).compareTo(minimum) < 0) {
                String lowest = formatter.format(convertValue(minimum, category.baseUnit(), from));
                throw new InvalidInputException("%s cannot be below %s %s"
                        .formatted(category.name(), lowest, from.symbol()));
            }
        });
    }
}
