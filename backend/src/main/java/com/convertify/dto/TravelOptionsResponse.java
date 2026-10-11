package com.convertify.dto;

import java.util.List;

/** Units and defaults for the time/distance/speed calculator form. */
public record TravelOptionsResponse(
        List<UnitDto> distanceUnits,
        List<UnitDto> speedUnits,
        List<UnitDto> timeUnits,
        String defaultDistanceUnit,
        String defaultSpeedUnit,
        String defaultTimeUnit) {
}
