package com.convertify.dto;

import java.util.List;

/** Every unit category and preset available to the generic converters. */
public record UnitCatalogResponse(List<Category> categories, List<Preset> presets) {

    public record Category(String code, String name, String baseUnit, boolean allowNegative, List<UnitDto> units) {
    }

    public record Preset(String code, String name, String category, List<UnitDto> units,
                         String defaultFrom, String defaultTo) {
    }
}
