package com.convertify.service;

import com.convertify.config.LandProperties;
import com.convertify.converter.UnitConversionEngine;
import com.convertify.converter.UnitDefinitionFactory;
import com.convertify.converter.UnitRegistry;
import com.convertify.dto.LandConversionRequest;
import com.convertify.dto.LandConversionResponse;
import com.convertify.dto.LandRegionsResponse;
import com.convertify.dto.QuantityDto;
import com.convertify.exception.ErrorCode;
import com.convertify.exception.InvalidInputException;
import com.convertify.model.ConversionResult;
import com.convertify.model.LandRegion;
import com.convertify.model.UnitCategory;
import com.convertify.model.UnitDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Land-area conversion with region-aware traditional units. Each region's unit set is the standard
 * AREA category plus that region's own definitions, and conversion reuses the generic engine.
 */
@Service
public class LandAreaService {

    private static final Logger log = LoggerFactory.getLogger(LandAreaService.class);

    private final UnitConversionEngine engine;
    private final ConversionResponseMapper mapper;
    private final LandProperties properties;
    private final Map<String, LandRegion> regions;
    private final LandRegionsResponse regionsResponse;

    public LandAreaService(LandProperties properties, UnitRegistry registry, UnitDefinitionFactory factory,
                           UnitConversionEngine engine, ConversionResponseMapper mapper) {
        this.engine = engine;
        this.mapper = mapper;
        this.properties = properties;
        UnitCategory area = registry.category(properties.areaCategory());

        Map<String, LandRegion> regionMap = new LinkedHashMap<>();
        for (LandProperties.Region source : properties.regions()) {
            List<UnitDefinition> units = new ArrayList<>(area.units());
            source.units().stream().map(factory::create).forEach(units::add);
            UnitCategory regionUnits = new UnitCategory(area.code(), area.name(), area.baseUnit().code(),
                    area.allowNegative(), area.minimumBaseValue().orElse(null), units);
            regionMap.put(key(source.code()), new LandRegion(source.code(), source.name(), source.description(),
                    regionUnits));
        }
        this.regions = Collections.unmodifiableMap(regionMap);
        if (!regions.containsKey(key(properties.defaultRegion()))) {
            throw new IllegalStateException("Default land region '%s' is not defined".formatted(properties.defaultRegion()));
        }
        this.regionsResponse = buildRegionsResponse();
        log.info("Land regions loaded: {} (default {})", regions.size(), properties.defaultRegion());
    }

    public LandRegionsResponse regions() {
        return regionsResponse;
    }

    public LandConversionResponse convert(LandConversionRequest request) {
        if (request.to() == null || request.to().isBlank()) {
            throw new InvalidInputException("Target unit is required");
        }
        LandRegion region = region(request.region());
        log.info("Land conversion requested: {} -> {} (region {})", request.from(), request.to(), region.code());
        ConversionResult result = engine.convert(region.units(), request.value(), request.from(), request.to());
        return new LandConversionResponse(
                regionRef(region),
                mapper.input(result.value(), result.from()),
                mapper.quantity(result.result(), result.to()),
                mapper.number(result.unitRate()),
                null,
                notes(Stream.of(result.from(), result.to()), region),
                properties.disclaimer());
    }

    public LandConversionResponse convertAll(LandConversionRequest request) {
        LandRegion region = region(request.region());
        log.info("Land multi-unit conversion requested: {} -> all units (region {})", request.from(), region.code());
        List<ConversionResult> results = engine.convertAll(region.units(), request.value(), request.from(), List.of());
        UnitDefinition from = region.units().find(request.from()).orElseThrow();
        List<QuantityDto> quantities = results.stream().map(r -> mapper.quantity(r.result(), r.to())).toList();
        return new LandConversionResponse(
                regionRef(region),
                mapper.input(request.value(), from),
                null,
                null,
                quantities,
                notes(region.units().units().stream(), region),
                properties.disclaimer());
    }

    private LandRegion region(String code) {
        LandRegion region = code == null ? null : regions.get(key(code));
        if (region == null) {
            throw new InvalidInputException(ErrorCode.INVALID_REGION, "Region '%s' is not supported".formatted(code));
        }
        return region;
    }

    private static List<String> notes(Stream<UnitDefinition> units, LandRegion region) {
        return units.filter(UnitDefinition::isRegional)
                .distinct()
                .map(unit -> "%s (%s)".formatted(unit.basis(), region.name()))
                .toList();
    }

    private static LandConversionResponse.RegionRef regionRef(LandRegion region) {
        return new LandConversionResponse.RegionRef(region.code(), region.name());
    }

    private LandRegionsResponse buildRegionsResponse() {
        List<LandRegionsResponse.Region> list = regions.values().stream()
                .map(region -> new LandRegionsResponse.Region(region.code(), region.name(), region.description(),
                        mapper.units(region.units())))
                .toList();
        LandRegion defaultRegion = regions.get(key(properties.defaultRegion()));
        return new LandRegionsResponse(defaultRegion.code(), properties.disclaimer(), list);
    }

    private static String key(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
