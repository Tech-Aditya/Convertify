package com.convertify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Area conversion within a region. {@code notes} spell out the conversion basis of any
 * region-dependent units involved; {@code result}/{@code unitRate} are absent for convert-all,
 * {@code results} is absent for a single conversion.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LandConversionResponse(
        RegionRef region,
        QuantityDto input,
        QuantityDto result,
        String unitRate,
        List<QuantityDto> results,
        List<String> notes,
        String disclaimer) {

    public record RegionRef(String code, String name) {
    }
}
