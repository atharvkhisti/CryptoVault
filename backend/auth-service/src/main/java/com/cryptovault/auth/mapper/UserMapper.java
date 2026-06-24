package com.cryptovault.auth.mapper;

import com.cryptovault.auth.entity.User;
import com.cryptovault.auth.dto.response.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper component converting {@link User} entities to {@link UserResponse} DTOs.
 * Ensures credential data is omitted.
 */
@Component
public class UserMapper {

    /**
     * Maps User entity to UserResponse DTO.
     *
     * @param user the User entity
     * @return the mapped UserResponse DTO
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
