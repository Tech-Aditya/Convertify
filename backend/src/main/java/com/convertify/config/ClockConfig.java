package com.convertify.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** A single injectable clock so "now" is consistent (and replaceable) across services. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
