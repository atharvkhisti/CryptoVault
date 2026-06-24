package com.cryptovault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Standard generic response envelope used to format unified REST API responses.
 *
 * @param <T> the type of response data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic REST API Response Envelope")
public class ApiResponse<T> {

    @Schema(description = "Response outcome success status flag", example = "true")
    private boolean success;

    @Schema(description = "Response status description message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data payload body")
    private T data;

    @Builder.Default
    @Schema(description = "Response generation epoch timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Factory helper to instantiate a successful response wrapper.
     *
     * @param message description of transaction outcome
     * @param data response payload
     * @param <T> payload type
     * @return successful ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Factory helper to instantiate a failed response wrapper.
     *
     * @param message description of error
     * @param <T> payload type
     * @return error ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
