package com.convertify.config;

/** Versioned REST path constants shared by controllers and web infrastructure. */
public final class ApiPaths {

    public static final String API_ROOT = "/api";
    public static final String V1 = API_ROOT + "/v1";

    public static final String INFO = V1 + "/info";
    public static final String UNITS = V1 + "/units";
    public static final String CURRENCY = V1 + "/currency";
    public static final String AREA = V1 + "/area";
    public static final String TIME_DISTANCE = V1 + "/time-distance";
    public static final String DATE = V1 + "/date";

    private ApiPaths() {
    }
}
