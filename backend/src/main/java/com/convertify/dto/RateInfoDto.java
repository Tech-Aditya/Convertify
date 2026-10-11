package com.convertify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Provenance of exchange-rate data.
 *
 * @param stale true when the provider could not be reached and older cached rates are being used
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RateInfoDto(String source, Instant lastUpdated, Instant nextUpdate, Instant fetchedAt, boolean stale) {
}
