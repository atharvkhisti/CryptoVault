package com.cryptovault.gateway.exception;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h3>GlobalExceptionHandler</h3>
 *
 * <p><b>Why it exists:</b> Intercepts exceptions thrown at the gateway controllers or filter layers, returning uniform REST payloads.</p>
 * <p><b>Architectural Layer:</b> Cross-Cutting / Error-Handling Layer.</p>
 * <p><b>Design Patterns Used:</b> Exception Handler / Interceptor Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Hides internal application implementation details (stack traces) from potential attackers, preventing information leakage.</p>
 * <p><b>Enterprise Relevance:</b> Enforces client contracts by formatting all responses using the standard {@link ApiResponse} wrapper.</p>
 * <p><b>Interview Talking Points:</b> Translates domain-specific exceptions (such as {@link UnauthorizedException}) into clean JSON envelopes with explicit HTTP response codes.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("Unauthorized access intercepted at gateway: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTokenException(InvalidTokenException ex) {
        log.warn("Invalid security token processed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.error("Gateway business rule exception: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Internal fallback error captured at gateway: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
