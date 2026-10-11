package com.convertify.exception;

import com.convertify.dto.ErrorResponse;
import com.convertify.dto.ErrorResponse.FieldViolation;
import com.convertify.logging.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;

/**
 * Translates every failure into an {@link ErrorResponse}. Users receive a safe, readable message;
 * technical detail (stack traces, provider errors) only goes to the log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ConvertifyException.class)
    public ResponseEntity<ErrorResponse> handleConvertify(ConvertifyException ex, HttpServletRequest request) {
        if (ex.errorCode().status().is5xxServerError()) {
            log.error("{} on {}: {}", ex.errorCode(), request.getRequestURI(), ex.getMessage());
        } else {
            log.warn("{} on {}: {}", ex.errorCode(), request.getRequestURI(), ex.getMessage());
        }
        return build(ex.errorCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<FieldViolation> violations = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> new FieldViolation(error.getField(), messageOf(error))),
                        ex.getBindingResult().getGlobalErrors().stream()
                                .map(error -> new FieldViolation(error.getObjectName(), error.getDefaultMessage())))
                .toList();
        log.warn("Validation failed on {}: {}", request.getRequestURI(), violations);
        return build(ErrorCode.VALIDATION_FAILED, summarise(violations), request, violations);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException ex,
                                                                   HttpServletRequest request) {
        List<FieldViolation> violations = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        log.warn("Parameter validation failed on {}: {}", request.getRequestURI(), violations);
        return build(ErrorCode.VALIDATION_FAILED, summarise(violations), request, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        log.warn("Unreadable request body on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        String message = "Request body is missing or malformed";
        if (ex.getMostSpecificCause() instanceof JacksonException jacksonException) {
            String field = jacksonException.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.joining("."));
            if (!field.isEmpty()) {
                message = "Invalid value for '%s'".formatted(field);
            }
        }
        return build(ErrorCode.MALFORMED_REQUEST, message, request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("No resource for {} {}", request.getMethod(), request.getRequestURI());
        return build(ErrorCode.NOT_FOUND, "The requested resource was not found", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex,
                                                      HttpServletRequest request) {
        log.debug("Method {} not supported on {}", ex.getMethod(), request.getRequestURI());
        return build(ErrorCode.METHOD_NOT_ALLOWED,
                "HTTP method %s is not supported for this endpoint".formatted(ex.getMethod()), request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                         HttpServletRequest request) {
        log.debug("Unsupported content type {} on {}", ex.getContentType(), request.getRequestURI());
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Content type must be application/json", request, List.of());
    }

    /** Anything unanticipated: full stack trace in the logs, a generic message to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected {} while handling {} {}: {}", ex.getClass().getSimpleName(), request.getMethod(),
                request.getRequestURI(), ex.getMessage(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "Something went wrong. Please try again.", request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, HttpServletRequest request,
                                                List<FieldViolation> violations) {
        ErrorResponse body = new ErrorResponse(Instant.now(clock), code.status().value(), code.name(), message,
                request.getRequestURI(), MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID), violations);
        return ResponseEntity.status(code.status()).body(body);
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid";
    }

    private static String summarise(List<FieldViolation> violations) {
        return violations.isEmpty()
                ? "Request validation failed"
                : violations.stream().map(FieldViolation::message).distinct().collect(Collectors.joining("; "));
    }
}
