package com.cryptovault.audit.exception;

import com.cryptovault.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * <h3>GlobalExceptionHandler</h3>
 *
 * <p><b>Why it exists:</b> Catches all uncaught domain, validation, and system exceptions microservice-wide, translating them into standardized JSON {@link ApiResponse} envelopes.</p>
 * <p><b>Architectural Layer:</b> Controller / Exception Translation Layer.</p>
 * <p><b>Compliance Relevance:</b> Prevents raw Java stack traces from leaking via REST API responses, protecting internal path names, schemas, and security internals from potential exploits (OWASP Top 10 mitigation).</p>
 * <p><b>Event-Driven Integration Path:</b> Catches errors during manual rest ingestion, incrementing observability failure metrics.</p>
 * <p><b>Enterprise Patterns Used:</b> Controller Advice Pattern / Interceptor Pattern.</p>
 * <p><b>Interview Talking Points:</b> Uses <code>@RestControllerAdvice</code> to declare global exception mappings. Returns unified {@link ApiResponse} objects, extracting validation constraints errors cleanly to prevent raw trace leaks.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Translates log not found exceptions into a HTTP 404 response.
     *
     * @param ex the log not found exception
     * @return the error response entity
     */
    @ExceptionHandler(AuditLogNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(AuditLogNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Translates invalid payload fields exceptions into a HTTP 400 response.
     *
     * @param ex the binding validation exception
     * @return the error response entity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> String.format("%s: %s", err.getField(), err.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + validationErrors));
    }

    /**
     * Handles illegal arguments or invalid parameter exceptions.
     *
     * @param ex the exception details
     * @return the error response entity
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid request parameters: " + ex.getMessage()));
    }

    /**
     * Catches global fallback system exceptions.
     *
     * @param ex the system exception
     * @return the generic HTTP 500 error response entity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
