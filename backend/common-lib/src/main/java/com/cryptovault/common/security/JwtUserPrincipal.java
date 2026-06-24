package com.cryptovault.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.security.Principal;
import java.util.UUID;

/**
 * Custom implementation of {@link Principal} representing the authenticated user context
 * extracted from JWT tokens, shared across microservices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserPrincipal implements Principal {

    private UUID userId;
    private String email;
    private String role;

    @Override
    public String getName() {
        return email;
    }
}
