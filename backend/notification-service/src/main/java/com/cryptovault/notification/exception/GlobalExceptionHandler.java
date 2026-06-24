package com.cryptovault.notification.exception;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * <h3>GlobalExceptionHandler</h3>
 *
 * <p><b>Why it exists:</b> Intercepts all REST layer exceptions thrown in the service, translating them into uniform, sanitized JSON responses.</p>
 * <p><b>Architectural Layer:</b> Cross-Cutting Concerns / Exception Layer.</p>
 * <p><b>Design Patterns Used:</b> Controller Advice / Exception Interceptor Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Sanitizes responses to block stack trace leakage, preventing potential attackers from inspecting server properties.</p>
 * <p><b>Future AWS Integration Path:</b> Acts as the exception boundary logger for programmatic gateway interactions.</p>
 * <p><b>Enterprise Relevance:</b> Enforces standard contract schemas across the entire microservice landscape, guaranteeing clients receive structured responses.</p>
 * <p><b>Interview Talking Points:</b> Collects payload validation errors (like mail formatting checks in DTOs) and packages them into standard {@link ApiResponse} envelopes with BAD_REQUEST statuses.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotificationNotFound(NotificationNotFoundException ex) {
        log.warn("Notification not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.error("Business validation failure: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed for parameters: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.success("Input parameters validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception intercepted: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred."));
    }
}
