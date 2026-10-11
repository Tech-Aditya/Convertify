package com.convertify.dto;

/** Which weekend and holiday rules were applied to a business-day calculation. */
public record CalendarInfoDto(String weekend, String holidays) {
}
