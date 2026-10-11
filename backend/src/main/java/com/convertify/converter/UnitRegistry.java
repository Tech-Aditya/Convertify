package com.convertify.converter;

import com.convertify.config.UnitCatalogProperties;
import com.convertify.exception.UnsupportedUnitException;
import com.convertify.model.UnitCategory;
import com.convertify.model.UnitDefinition;
import com.convertify.model.UnitPreset;
import com.convertify.util.DecimalExpressions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of every unit category and preset, built once from the catalog at startup.
 * Misconfiguration (unknown base unit, duplicate codes, bad factors) fails fast on boot.
 */
@Component
public class UnitRegistry {

    private static final Logger log = LoggerFactory.getLogger(UnitRegistry.class);

    private final Map<String, UnitCategory> categories;
    private final Map<String, UnitPreset> presets;

    public UnitRegistry(UnitCatalogProperties catalog, UnitDefinitionFactory factory) {
        Map<String, UnitCategory> categoryMap = new LinkedHashMap<>();
        for (UnitCatalogProperties.Category source : catalog.categories()) {
            List<UnitDefinition> units = source.units().stream().map(factory::create).toList();
            UnitCategory category = new UnitCategory(
                    source.code(),
                    source.name(),
                    source.baseUnit(),
                    source.allowNegative(),
                    source.minimumBaseValue() == null || source.minimumBaseValue().isBlank()
                            ? null
                            : DecimalExpressions.parse(source.minimumBaseValue(), factory.mathContext()),
                    units);
            if (categoryMap.putIfAbsent(key(source.code()), category) != null) {
                throw new IllegalStateException("Duplicate unit category '%s'".formatted(source.code()));
            }
        }
        this.categories = Collections.unmodifiableMap(categoryMap);

        Map<String, UnitPreset> presetMap = new LinkedHashMap<>();
        for (UnitCatalogProperties.Preset source : catalog.presets()) {
            UnitCategory category = category(source.category());
            UnitPreset preset = new UnitPreset(
                    source.code(),
                    source.name(),
                    category,
                    source.units().stream().map(code -> unit(category, code)).toList(),
                    unit(category, source.defaultFrom()),
                    unit(category, source.defaultTo()));
            presetMap.put(key(source.code()), preset);
        }
        this.presets = Collections.unmodifiableMap(presetMap);

        log.info("Unit catalog loaded: {} categories, {} units, {} presets",
                categories.size(), categories.values().stream().mapToInt(c -> c.units().size()).sum(), presets.size());
    }

    public UnitCategory category(String code) {
        UnitCategory category = code == null ? null : categories.get(key(code));
        if (category == null) {
            throw UnsupportedUnitException.category(code);
        }
        return category;
    }

    public UnitDefinition unit(UnitCategory category, String unitCode) {
        return category.find(unitCode)
                .orElseThrow(() -> UnsupportedUnitException.unit(unitCode, category.name().toLowerCase(Locale.ROOT)));
    }

    public List<UnitCategory> categories() {
        return List.copyOf(categories.values());
    }

    public List<UnitPreset> presets() {
        return List.copyOf(presets.values());
    }

    private static String key(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
