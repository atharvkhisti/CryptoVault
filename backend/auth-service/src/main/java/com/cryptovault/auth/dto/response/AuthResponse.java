package com.cryptovault.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the authentication response containing JWT access tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing the generated JWT token and context details")
public class AuthResponse {

    @Schema(description = "Symmetric HMAC-SHA256 signed JWT token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Builder.Default
    @Schema(description = "Authorization header scheme type", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Token time-to-live bounds duration in milliseconds", example = "86400000")
    private long expiresIn;
}
