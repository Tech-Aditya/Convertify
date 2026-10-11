package com.convertify.dto;

import java.util.List;

/** One input expressed in many units. */
public record ConvertAllResponse(String category, QuantityDto input, List<QuantityDto> results) {
}
