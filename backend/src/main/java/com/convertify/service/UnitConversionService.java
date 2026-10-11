package com.convertify.service;

import com.convertify.converter.UnitConversionEngine;
import com.convertify.converter.UnitRegistry;
import com.convertify.dto.ConversionResponse;
import com.convertify.dto.ConvertAllResponse;
import com.convertify.dto.UnitCatalogResponse;
import com.convertify.dto.UnitConversionRequest;
import com.convertify.dto.UnitConvertAllRequest;
import com.convertify.dto.UnitDto;
import com.convertify.model.ConversionResult;
import com.convertify.model.UnitCategory;
import com.convertify.model.UnitDefinition;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Generic unit conversion for every catalog category (length, speed, weight, temperature, ...). */
@Service
public class UnitConversionService {

    private static final Logger log = LoggerFactory.getLogger(UnitConversionService.class);

    private final UnitRegistry registry;
    private final UnitConversionEngine engine;
    private final ConversionResponseMapper mapper;
    private final UnitCatalogResponse catalog;

    public UnitConversionService(UnitRegistry registry, UnitConversionEngine engine, ConversionResponseMapper mapper) {
        this.registry = registry;
        this.engine = engine;
        this.mapper = mapper;
        this.catalog = buildCatalog();
    }

    /** The catalog is immutable, so the response is built once. */
    public UnitCatalogResponse catalog() {
        return catalog;
    }

    public ConversionResponse convert(UnitConversionRequest request) {
        UnitCategory category = registry.category(request.category());
        log.info("Unit conversion requested: {} {} -> {}", category.code(), request.from(), request.to());
        ConversionResult result = engine.convert(category, request.value(), request.from(), request.to());
        return mapper.toResponse(category.code(), result);
    }

    public ConvertAllResponse convertAll(UnitConvertAllRequest request) {
        UnitCategory category = registry.category(request.category());
        log.info("Multi-unit conversion requested: {} {} -> {} units", category.code(), request.from(),
                request.units() == null || request.units().isEmpty() ? "all" : request.units().size());
        List<ConversionResult> results = engine.convertAll(category, request.value(), request.from(), request.units());
        UnitDefinition from = registry.unit(category, request.from());
        return mapper.toResponse(category.code(), request.value(), from, results);
    }

    private UnitCatalogResponse buildCatalog() {
        List<UnitCatalogResponse.Category> categories = registry.categories().stream()
                .map(category -> new UnitCatalogResponse.Category(
                        category.code(),
                        category.name(),
                        category.baseUnit().code(),
                        category.allowNegative(),
                        mapper.units(category)))
                .toList();
        List<UnitCatalogResponse.Preset> presets = registry.presets().stream()
                .map(preset -> new UnitCatalogResponse.Preset(
                        preset.code(),
                        preset.name(),
                        preset.category().code(),
                        preset.units().stream().map(UnitDto::from).toList(),
                        preset.defaultFrom().code(),
                        preset.defaultTo().code()))
                .toList();
        return new UnitCatalogResponse(categories, presets);
    }
}
