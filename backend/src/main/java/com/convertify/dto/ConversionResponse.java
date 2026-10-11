package com.convertify.dto;

/**
 * Result of a single conversion.
 *
 * @param unitRate value of one input unit expressed in the result unit (e.g. 1 km = 0.621371 mi)
 */
public record ConversionResponse(String category, QuantityDto input, QuantityDto result, String unitRate) {
}
