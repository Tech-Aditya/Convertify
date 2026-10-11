package com.convertify.exception;

import org.springframework.http.HttpStatus;

/** Stable, machine-readable error codes returned in the {@code error} field of API errors. */
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_CATEGORY(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_UNIT(HttpStatus.BAD_REQUEST),
    INVALID_CURRENCY(HttpStatus.BAD_REQUEST),
    INVALID_REGION(HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    EXCHANGE_RATE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
