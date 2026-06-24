package com.cryptovault.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.cryptovault.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * DTO representing user profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload representing user profile details")
public class UserResponse {

    @Schema(description = "User unique UUID record identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "User registered display name", example = "Alice Developer")
    private String name;

    @Schema(description = "User unique login email address", example = "alice@cryptovault.com")
    private String email;

    @Schema(description = "User authorization clearance profile mapping", example = "USER")
    private Role role;
}
