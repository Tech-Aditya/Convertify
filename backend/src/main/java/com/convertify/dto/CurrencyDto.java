package com.convertify.dto;

/** A supported currency. {@code fractionDigits} is the number of minor-unit digits (2 for USD, 0 for JPY). */
public record CurrencyDto(String code, String name, int fractionDigits, boolean featured) {
}
