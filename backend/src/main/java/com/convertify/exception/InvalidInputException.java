package com.convertify.exception;

/** The request is well-formed but its values cannot be processed (range, sign, unknown code, ...). */
public class InvalidInputException extends ConvertifyException {

    public InvalidInputException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }

    public InvalidInputException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
