package com.cryptovault.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Data Transfer Object representing essential user details extracted from an authenticated
 * session (JWT) to be passed across microservice boundaries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContextDto {

    private UUID userId;
    private String email;
    private String role;
}
