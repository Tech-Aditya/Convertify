package com.convertify.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origins permitted to call the API when the frontend is hosted on a different origin. */
@ConfigurationProperties(prefix = "convertify.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList();
    }
}
