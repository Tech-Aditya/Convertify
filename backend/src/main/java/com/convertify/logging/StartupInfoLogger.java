package com.convertify.logging;

import com.convertify.config.ApiPaths;
import com.convertify.config.AppProperties;
import com.convertify.config.CurrencyProperties;
import com.convertify.config.DateProperties;
import com.convertify.config.LandProperties;
import com.convertify.util.Durations;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Logs a concise summary once the application is ready: versions, profile, where it is
 * reachable, and the status of important configuration. Secrets are never printed — the
 * currency provider is identified by host only and the API key by presence only.
 */
@Component
public class StartupInfoLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);
    private static final String RULE = "=".repeat(64);

    private final Environment environment;
    private final AppProperties app;
    private final CurrencyProperties currency;
    private final LandProperties land;
    private final DateProperties date;

    public StartupInfoLogger(Environment environment, AppProperties app, CurrencyProperties currency,
                             LandProperties land, DateProperties date) {
        this.environment = environment;
        this.app = app;
        this.currency = currency;
        this.land = land;
        this.date = date;
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "?"));
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String baseUrl = "http://localhost:" + port + contextPath;
        String profiles = String.join(", ", Arrays.asList(environment.getActiveProfiles()));
        Duration startup = event.getTimeTaken();

        log.info(RULE);
        log.info("{} — {}", app.name(), app.tagline());
        log.info(RULE);
        log.info("Version          : {}", app.version());
        log.info("Active profile   : {}", profiles.isEmpty() ? "default" : profiles);
        log.info("Java             : {} ({})", Runtime.version(), System.getProperty("java.vendor"));
        log.info("Spring Boot      : {}", SpringBootVersion.getVersion());
        log.info("Server port      : {}{}", port, contextPath.isEmpty() ? "" : " (context path " + contextPath + ")");
        log.info("Local URL        : {}/  (API {}{}, health {}/actuator/health)", baseUrl, baseUrl, ApiPaths.V1, baseUrl);
        log.info("Currency provider: {} [{}] via {}, API key {}, cache TTL {}",
                currency.provider().name(), currency.provider().type(), providerHost(),
                currency.provider().hasApiKey() ? "configured" : "not configured", Durations.human(currency.provider().cacheTtl()));
        log.info("Date / land      : time zone {}, default land region {}",
                date.defaultTimeZone(), land.defaultRegion());
        log.info("Log files        : {}", environment.acceptsProfiles(Profiles.of("container"))
                ? "none (container profile: stdout only)"
                : Path.of(environment.getProperty("convertify.logging.directory", "logs")).toAbsolutePath().normalize());
        log.info("Debug logging    : {}", LoggerFactory.getLogger("com.convertify").isDebugEnabled() ? "on" : "off");
        log.info(RULE);
        log.info("{} started successfully{}", app.name(),
                startup == null ? "" : " in %.2f seconds".formatted(startup.toMillis() / 1000.0));
    }

    /** Host of the provider URL template — never the full URL, which may embed the API key. */
    private String providerHost() {
        try {
            String host = URI.create(currency.provider().url().replaceAll("\\{[^}]*}", "x")).getHost();
            return host == null ? "unknown host" : host;
        } catch (IllegalArgumentException ex) {
            return "unparseable URL";
        }
    }
}
