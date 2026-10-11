package com.convertify.model;

/** Units a date can be shifted by. Business days skip weekends and holidays. */
public enum DateUnit {
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
    BUSINESS_DAYS
}
