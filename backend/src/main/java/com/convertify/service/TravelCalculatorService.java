package com.convertify.service;

import com.convertify.config.TravelProperties;
import com.convertify.converter.UnitConversionEngine;
import com.convertify.converter.UnitDefinitionFactory;
import com.convertify.converter.UnitRegistry;
import com.convertify.dto.QuantityDto;
import com.convertify.dto.TravelCalculationRequest;
import com.convertify.dto.TravelCalculationResponse;
import com.convertify.dto.TravelOptionsResponse;
import com.convertify.exception.InvalidInputException;
import com.convertify.model.TravelQuantity;
import com.convertify.model.UnitCategory;
import com.convertify.model.UnitDefinition;
import com.convertify.util.DurationFormatter;
import com.convertify.util.InputLimits;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Distance = Speed × Time. Every input is first normalised to a coherent unit set
 * (by default meters, meters per second and seconds) so any unit combination works.
 */
@Service
public class TravelCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(TravelCalculatorService.class);

    private final UnitRegistry registry;
    private final UnitConversionEngine engine;
    private final ConversionResponseMapper mapper;
    private final InputLimits inputLimits;
    private final TravelProperties properties;
    private final MathContext mathContext;

    private final UnitCategory distanceCategory;
    private final UnitCategory speedCategory;
    private final UnitCategory timeCategory;
    private final UnitDefinition meters;
    private final UnitDefinition metersPerSecond;
    private final UnitDefinition seconds;
    private final TravelOptionsResponse options;

    public TravelCalculatorService(UnitRegistry registry, UnitConversionEngine engine, ConversionResponseMapper mapper,
                                   InputLimits inputLimits, TravelProperties properties,
                                   UnitDefinitionFactory factory) {
        this.registry = registry;
        this.engine = engine;
        this.mapper = mapper;
        this.inputLimits = inputLimits;
        this.properties = properties;
        this.mathContext = factory.mathContext();
        this.distanceCategory = registry.category(properties.distanceCategory());
        this.speedCategory = registry.category(properties.speedCategory());
        this.timeCategory = registry.category(properties.timeCategory());
        this.meters = registry.unit(distanceCategory, properties.normalizedDistanceUnit());
        this.metersPerSecond = registry.unit(speedCategory, properties.normalizedSpeedUnit());
        this.seconds = registry.unit(timeCategory, properties.normalizedTimeUnit());
        this.options = new TravelOptionsResponse(
                mapper.units(distanceCategory),
                mapper.units(speedCategory),
                mapper.units(timeCategory),
                registry.unit(distanceCategory, properties.defaultDistanceUnit()).code(),
                registry.unit(speedCategory, properties.defaultSpeedUnit()).code(),
                registry.unit(timeCategory, properties.defaultTimeResultUnit()).code());
    }

    public TravelOptionsResponse options() {
        return options;
    }

    public TravelCalculationResponse calculate(TravelCalculationRequest request) {
        log.info("Travel calculation requested: solve for {} (units: distance {}, speed {}, time {})",
                request.solveFor(), request.distanceUnit(), request.speedUnit(), request.timeUnit());
        return switch (request.solveFor()) {
            case TIME -> solveTime(request);
            case DISTANCE -> solveDistance(request);
            case SPEED -> solveSpeed(request);
        };
    }

    private TravelCalculationResponse solveTime(TravelCalculationRequest request) {
        Known distance = known(request.distance(), request.distanceUnit(), distanceCategory, "Distance", false);
        Known speed = known(request.speed(), request.speedUnit(), speedCategory, "Speed", true);
        UnitDefinition resultUnit = resultUnit(request.timeUnit(), timeCategory, properties.defaultTimeResultUnit());

        BigDecimal totalSeconds = normalise(distance, meters).divide(normalise(speed, metersPerSecond), mathContext);
        BigDecimal result = engine.convertValue(totalSeconds, seconds, resultUnit);

        DurationFormatter.Breakdown breakdown = DurationFormatter.breakdown(totalSeconds);
        return new TravelCalculationResponse(
                TravelQuantity.TIME,
                TravelQuantity.TIME.formula(),
                mapper.quantity(result, resultUnit),
                DurationFormatter.humanise(totalSeconds),
                new TravelCalculationResponse.Duration(breakdown.days().toString(), breakdown.hours(),
                        breakdown.minutes(), breakdown.seconds()),
                inputs(distance, speed));
    }

    private TravelCalculationResponse solveDistance(TravelCalculationRequest request) {
        Known speed = known(request.speed(), request.speedUnit(), speedCategory, "Speed", false);
        Known time = known(request.time(), request.timeUnit(), timeCategory, "Time", false);
        UnitDefinition resultUnit = resultUnit(request.distanceUnit(), distanceCategory, null);

        BigDecimal totalMeters = normalise(speed, metersPerSecond).multiply(normalise(time, seconds), mathContext);
        return simple(TravelQuantity.DISTANCE, engine.convertValue(totalMeters, meters, resultUnit), resultUnit,
                speed, time);
    }

    private TravelCalculationResponse solveSpeed(TravelCalculationRequest request) {
        Known distance = known(request.distance(), request.distanceUnit(), distanceCategory, "Distance", false);
        Known time = known(request.time(), request.timeUnit(), timeCategory, "Time", true);
        UnitDefinition resultUnit = resultUnit(request.speedUnit(), speedCategory, null);

        BigDecimal speedMps = normalise(distance, meters).divide(normalise(time, seconds), mathContext);
        return simple(TravelQuantity.SPEED, engine.convertValue(speedMps, metersPerSecond, resultUnit), resultUnit,
                distance, time);
    }

    private TravelCalculationResponse simple(TravelQuantity quantity, BigDecimal value, UnitDefinition unit,
                                             Known first, Known second) {
        return new TravelCalculationResponse(quantity, quantity.formula(), mapper.quantity(value, unit), null, null,
                inputs(first, second));
    }

    private record Known(BigDecimal value, UnitDefinition unit) {
    }

    private Known known(BigDecimal value, String unitCode, UnitCategory category, String label, boolean mustBePositive) {
        if (value == null) {
            throw new InvalidInputException("%s is required".formatted(label));
        }
        if (mustBePositive) {
            inputLimits.requirePositive(value, label);
        } else {
            inputLimits.requireNonNegative(value, label);
        }
        return new Known(value, requiredUnit(unitCode, category, label));
    }

    private UnitDefinition resultUnit(String unitCode, UnitCategory category, String fallback) {
        String code = unitCode == null || unitCode.isBlank() ? fallback : unitCode;
        return requiredUnit(code, category, "Result");
    }

    private UnitDefinition requiredUnit(String unitCode, UnitCategory category, String label) {
        if (unitCode == null || unitCode.isBlank()) {
            throw new InvalidInputException("%s unit is required".formatted(label));
        }
        return registry.unit(category, unitCode);
    }

    private BigDecimal normalise(Known known, UnitDefinition target) {
        return engine.convertValue(known.value(), known.unit(), target);
    }

    private List<QuantityDto> inputs(Known first, Known second) {
        return List.of(mapper.input(first.value(), first.unit()), mapper.input(second.value(), second.unit()));
    }
}
