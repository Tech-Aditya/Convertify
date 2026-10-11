package com.convertify.dto;

import com.convertify.model.UnitDefinition;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Public view of a unit. {@code basis} is present only for region-dependent units. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UnitDto(String code, String name, String symbol, String basis) {

    public static UnitDto from(UnitDefinition unit) {
        return new UnitDto(unit.code(), unit.name(), unit.symbol(), unit.isRegional() ? unit.basis() : null);
    }
}
