package com.convertify.exception;

/** A unit or unit category that is not defined in the catalog. */
public class UnsupportedUnitException extends ConvertifyException {

    private UnsupportedUnitException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static UnsupportedUnitException unit(String unitCode, String categoryName) {
        return new UnsupportedUnitException(ErrorCode.UNSUPPORTED_UNIT,
                "Unit '%s' is not supported for %s".formatted(unitCode, categoryName));
    }

    public static UnsupportedUnitException category(String categoryCode) {
        return new UnsupportedUnitException(ErrorCode.UNSUPPORTED_CATEGORY,
                "Unit category '%s' is not supported".formatted(categoryCode));
    }
}
