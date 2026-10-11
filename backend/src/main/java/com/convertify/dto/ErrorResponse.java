package com.convertify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Uniform error body returned by every API endpoint.
 *
 * @param requestId correlation id of the failed request; quote it to find the matching log lines
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId,
        List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {
    }
}
