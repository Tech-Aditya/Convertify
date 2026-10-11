package com.convertify.dto;

/**
 * A number with its unit. {@code value} is a plain decimal string (already rounded for display)
 * so no precision is lost to JSON floating-point parsing.
 */
public record QuantityDto(String value, UnitDto unit) {
}
