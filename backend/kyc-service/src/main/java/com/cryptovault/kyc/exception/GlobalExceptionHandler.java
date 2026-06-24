package com.cryptovault.kyc.exception;

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
 * <p><b>Why it exists:</b> Centralizes exception interception and formats them into clean standard {@link ApiResponse} envelopes.</p>
 * <p><b>Architectural Layer:</b> Controller / Exception Translation Layer.</p>
 * <p><b>Design Patterns Used:</b> Controller Advice Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Hides internal stack traces and database pathways, complying with OWASP security requirements regarding exception leakage.</p>
 * <p><b>Scalability Considerations:</b> Handled entirely in-memory with very low resource overhead.</p>
 * <p><b>Interview Talking Points:</b> Maps custom <code>KycException</code>, validation failures, and generic exceptions to type-safe <code>ApiResponse</code> JSON wrappers.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(KycRecordNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleKycRecordNotFound(KycRecordNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocumentNotFound(DocumentNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(KycException.class)
    public ResponseEntity<ApiResponse<Void>> handleKycException(KycException ex) {
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
                .body(ApiResponse.error("Invalid parameters: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
