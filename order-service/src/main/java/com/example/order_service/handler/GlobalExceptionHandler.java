package com.example.order_service.handler;

import com.example.order_service.common.ApiResponse;
import com.example.order_service.common.ErrorCode;
import com.example.order_service.common.Messages;
import com.example.order_service.exceptions.*;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The one and only exception handler for the whole application.
 * It inherits from ResponseEntityExceptionHandler to handle Spring MVC exceptions (like 405, 415, wrong json format and missing params).
 * and it doesn't be converted to 500 wrongly, but instead it returns a proper status code and a unified error response.
 * and it also handles our custom AppException (NotFound, Business, Conflict, Unauthorized...).
 *
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Messages messages;

    // One place that builds every error body; the HTTP status comes from the ErrorCode.
    private ApiResponse<Void> fail(HttpServletResponse response, ErrorCode code, Object... args) {
        response.setStatus(code.getStatus().value());
        return ApiResponse.error(code.name(), messages.get(code, args));
    }

    private ApiResponse<Map<String, String>> validationFailed(HttpServletResponse response, Map<String, String> errors) {
        response.setStatus(ErrorCode.VALIDATION_FAILED.getStatus().value());
        return ApiResponse.error(ErrorCode.VALIDATION_FAILED.name(),
                messages.get(ErrorCode.VALIDATION_FAILED), errors);
    }

    // Every AppException: not found, conflict, service unavailable...
    @ExceptionHandler(AppException.class)
    public ApiResponse<Void> handleApp(AppException ex, HttpServletResponse response) {
        var code = ex.getErrorCode();
        if (code.getStatus().is5xxServerError()) {
            // the client gets a generic message, the details stay in the log
            log.warn("args={} cause={}", code, Arrays.toString(ex.getArgs()), ex.getCause());
        }
        return fail(response, code, (Object[]) ex.getArgs());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletResponse response) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return validationFailed(response, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletResponse response) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(v -> errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return validationFailed(response, errors);
    }

    // broken JSON, or a wrong type in the path / query: the client's mistake (400), not a 500
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ApiResponse<Void> handleBadInput(Exception ex, HttpServletResponse response) {
        return fail(response, ErrorCode.VALIDATION_FAILED);
    }

    // DB constraint errors. Unique (23505) and foreign key (23503) are conflicts: 409.
    // Anything else (for example NOT NULL, 23502) means OUR code is wrong, so it stays a 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<Void> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletResponse response) {
        if (ex.getMostSpecificCause() instanceof SQLException sql
                && Set.of("23505", "23503").contains(sql.getSQLState())) {
            log.warn("Data conflict: {}", sql.getMessage());     // logged, NOT sent to the client
            return fail(response, ErrorCode.DATA_CONFLICT);
        }
        return handleUnexpected(ex, response);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ApiResponse<Void> handleResourceFailure(DataAccessResourceFailureException ex, HttpServletResponse response) {
        log.warn("Database is not reachable: {}", ex.getMessage());
        return fail(response, ErrorCode.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception ex, HttpServletResponse response) {
        // Spring MVC exceptions (405, 415, unknown path, missing parameter...) carry their own status
        if (ex instanceof ErrorResponse spring) {
            int status = spring.getStatusCode().value();
            if (spring.getStatusCode().is5xxServerError()) {
                log.error("Server error", ex);
            }
            response.setStatus(status);
            HttpStatus known = HttpStatus.resolve(status);
            return ApiResponse.error("HTTP_" + status, known != null ? known.getReasonPhrase() : "Error");
        }
        log.error("Unexpected error", ex);
        return fail(response, ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ApiResponse<Void> handleDependencyUnavailable(
            DependencyUnavailableException e,
            HttpServletResponse response) {

        log.warn("Dependency unavailable: {}", e.getMessage());

        return fail(response, ErrorCode.SERVICE_UNAVAILABLE);
    }

    // Let Spring Security answer with its own 403 handler instead of the catch-all returning 500
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }
}