package com.convertify.converter;

import com.convertify.config.ConversionProperties;
import com.convertify.config.UnitDefinitionProperties;
import com.convertify.model.UnitDefinition;
import com.convertify.util.DecimalExpressions;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Turns raw catalog entries into validated {@link UnitDefinition}s. */
@Component
public class UnitDefinitionFactory {

    private final MathContext mathContext;

    public UnitDefinitionFactory(ConversionProperties properties) {
        this.mathContext = properties.mathContext();
    }

    public UnitDefinition create(UnitDefinitionProperties source) {
        try {
            return new UnitDefinition(
                    source.code().trim(),
                    source.name(),
                    source.symbol(),
                    DecimalExpressions.parse(source.factor(), mathContext),
                    source.offset() == null || source.offset().isBlank()
                            ? null
                            : DecimalExpressions.parse(source.offset(), mathContext),
                    source.basis());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid unit definition '%s': %s".formatted(source.code(), ex.getMessage()), ex);
        }
    }

    public MathContext mathContext() {
        return mathContext;
    }
}
