package com.convertify.dto;

import java.util.List;

/** Supported currencies (featured first) with default selections and rate provenance. */
public record CurrencyListResponse(
        String defaultFrom,
        String defaultTo,
        List<CurrencyDto> currencies,
        RateInfoDto rates) {
}
