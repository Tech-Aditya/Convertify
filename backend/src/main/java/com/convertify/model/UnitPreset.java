package com.convertify.model;

import java.util.List;

/** A curated subset of a category's units used by a dedicated calculator card (e.g. Height). */
public record UnitPreset(
        String code,
        String name,
        UnitCategory category,
        List<UnitDefinition> units,
        UnitDefinition defaultFrom,
        UnitDefinition defaultTo) {
}
