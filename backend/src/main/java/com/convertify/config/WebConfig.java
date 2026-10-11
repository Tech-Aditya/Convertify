package com.convertify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Cross-origin access for a separately hosted frontend. Same-origin hosting needs none. */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final CorsProperties corsProperties;
    private final LoggingProperties loggingProperties;

    public WebConfig(CorsProperties corsProperties, LoggingProperties loggingProperties) {
        this.corsProperties = corsProperties;
        this.loggingProperties = loggingProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsProperties.allowedOrigins().isEmpty()) {
            log.debug("CORS disabled: frontend is expected on the same origin");
            return;
        }
        log.info("CORS enabled for origins {}", corsProperties.allowedOrigins());
        registry.addMapping(ApiPaths.API_ROOT + "/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", loggingProperties.requestIdHeader())
                .exposedHeaders(loggingProperties.requestIdHeader())
                .maxAge(3600);
    }
}
