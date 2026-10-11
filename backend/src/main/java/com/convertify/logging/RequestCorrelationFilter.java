package com.convertify.logging;

import com.convertify.config.ApiPaths;
import com.convertify.config.LoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Correlation and access logging for API requests.
 * <ul>
 *   <li>Uses the caller's request id header when it is well-formed, otherwise generates one;
 *       the id is put in the MDC (so every log line of the request carries it) and echoed back.</li>
 *   <li>Logs method, path, status, outcome and duration once the request completes; slow
 *       requests are logged at WARN.</li>
 * </ul>
 * Headers, query strings and bodies are never logged, so API keys, tokens and personal data
 * cannot leak through access logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    /** Accept only short, safe ids from clients: they are written into every log line. */
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    private final LoggingProperties properties;

    public RequestCorrelationFilter(LoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ApiPaths.API_ROOT + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(properties.requestIdHeader()));
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(properties.requestIdHeader(), requestId);

        String method = request.getMethod();
        String path = request.getRequestURI();
        long start = System.nanoTime();
        log.debug("{} {} received", method, path);
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            logCompletion(method, path, response.getStatus(), elapsedMs);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private void logCompletion(String method, String path, int status, long elapsedMs) {
        boolean slow = elapsedMs >= properties.slowRequestThreshold().toMillis();
        if (status >= 500) {
            log.warn("{} {} failed with {} in {} ms", method, path, status, elapsedMs);
        } else if (status >= 400) {
            log.info("{} {} rejected with {} in {} ms", method, path, status, elapsedMs);
        } else if (slow) {
            log.warn("{} {} completed with {} in {} ms (slow; threshold {} ms)", method, path, status, elapsedMs,
                    properties.slowRequestThreshold().toMillis());
        } else {
            log.info("{} {} completed with {} in {} ms", method, path, status, elapsedMs);
        }
    }

    static String resolveRequestId(String supplied) {
        if (supplied != null && SAFE_REQUEST_ID.matcher(supplied).matches()) {
            return supplied;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
