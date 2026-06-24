package com.cryptovault.risk.exception;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * <h3>GlobalExceptionHandler</h3>
 *
 * <p><b>Why it exists:</b> Centralizes exception interception and formats them into clean {@link ApiResponse} envelopes.</p>
 * <p><b>Architectural Layer:</b> Controller / Exception Translation Layer.</p>
 * <p><b>Design Patterns Used:</b> Controller Advice Pattern, Interceptor Pattern.</p>
 * <p><b>Banking Relevance:</b> Secures application logs and limits raw runtime details leakage to prevent security exploits (OWASP compliance).</p>
 * <p><b>Scalability Considerations:</b> Handled in-memory locally with minimal overhead.</p>
 * <p><b>Interview Talking Points:</b> Translates validation checks and domain-specific <code>BusinessException</code> instances into unified error JSON envelopes.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RiskAssessmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RiskAssessmentNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> String.format("%s: %s", err.getField(), err.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + validationErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid request parameters: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
