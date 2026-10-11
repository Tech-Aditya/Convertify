package com.convertify.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * An immutable, ordered set of mutually convertible units sharing one base unit.
 * Unit codes are matched case-insensitively.
 */
public final class UnitCategory {

    private final String code;
    private final String name;
    private final UnitDefinition baseUnit;
    private final boolean allowNegative;
    private final BigDecimal minimumBaseValue;
    private final Map<String, UnitDefinition> unitsByKey;

    public UnitCategory(String code, String name, String baseUnitCode, boolean allowNegative,
                        BigDecimal minimumBaseValue, List<UnitDefinition> units) {
        this.code = code;
        this.name = name;
        this.allowNegative = allowNegative;
        this.minimumBaseValue = minimumBaseValue;
        Map<String, UnitDefinition> map = new LinkedHashMap<>();
        for (UnitDefinition unit : units) {
            if (map.putIfAbsent(key(unit.code()), unit) != null) {
                throw new IllegalStateException("Duplicate unit '%s' in category %s".formatted(unit.code(), code));
            }
        }
        this.unitsByKey = Collections.unmodifiableMap(map);
        this.baseUnit = find(baseUnitCode).orElseThrow(() -> new IllegalStateException(
                "Base unit '%s' is not defined in category %s".formatted(baseUnitCode, code)));
    }

    public Optional<UnitDefinition> find(String unitCode) {
        return unitCode == null ? Optional.empty() : Optional.ofNullable(unitsByKey.get(key(unitCode)));
    }

    public List<UnitDefinition> units() {
        return List.copyOf(unitsByKey.values());
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public UnitDefinition baseUnit() {
        return baseUnit;
    }

    public boolean allowNegative() {
        return allowNegative;
    }

    public Optional<BigDecimal> minimumBaseValue() {
        return Optional.ofNullable(minimumBaseValue);
    }

    private static String key(String unitCode) {
        return unitCode.trim().toLowerCase(Locale.ROOT);
    }
}
