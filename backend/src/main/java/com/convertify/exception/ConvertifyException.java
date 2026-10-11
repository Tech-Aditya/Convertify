package com.convertify.exception;

/**
 * Base class for expected, user-facing failures. The message must be safe to show to end users;
 * technical detail belongs in the cause and the logs.
 */
public class ConvertifyException extends RuntimeException {

    private final ErrorCode errorCode;

    public ConvertifyException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ConvertifyException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
