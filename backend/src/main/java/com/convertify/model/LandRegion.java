package com.convertify.model;

/**
 * A region together with its effective area category: the standard AREA units plus the
 * region's traditional units (whose values depend on the region).
 */
public record LandRegion(String code, String name, String description, UnitCategory units) {
}
