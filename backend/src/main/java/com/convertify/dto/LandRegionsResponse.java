package com.convertify.dto;

import java.util.List;

/** Land regions and the area units available in each (standard units first, then regional ones). */
public record LandRegionsResponse(String defaultRegion, String disclaimer, List<Region> regions) {

    public record Region(String code, String name, String description, List<UnitDto> units) {
    }
}
