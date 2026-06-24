package com.cryptovault.kyc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * <h3>UserResponse</h3>
 *
 * <p><b>Why it exists:</b> DTO mapping of user profiles returned from the auth-service.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private String role;
}
