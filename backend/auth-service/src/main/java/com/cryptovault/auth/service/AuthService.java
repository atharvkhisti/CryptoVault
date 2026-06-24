package com.cryptovault.auth.service;

import com.cryptovault.auth.dto.request.LoginRequest;
import com.cryptovault.auth.dto.request.RegisterRequest;
import com.cryptovault.auth.dto.response.AuthResponse;
import com.cryptovault.auth.dto.response.UserResponse;
import com.cryptovault.auth.entity.User;
import com.cryptovault.auth.enums.Role;
import com.cryptovault.auth.exception.EmailAlreadyExistsException;
import com.cryptovault.auth.exception.InvalidCredentialsException;
import com.cryptovault.auth.mapper.UserMapper;
import com.cryptovault.auth.repository.UserRepository;
import com.cryptovault.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Service class orchestrating authentication, login validation,
 * password cryptography, and user profile persistence mapping.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user inside the CryptoVault platform.
     *
     * @param request the registration details
     * @return the UserResponse representing profile information
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email address is already registered: " + request.getEmail());
        }

        validatePasswordStrength(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Defaults to USER role
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    /**
     * Authenticates an existing user and issues JWT security access token.
     *
     * @param request credentials request
     * @return the access token details
     */
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationTime())
                    .build();
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    /**
     * Extracts currently authenticated user from SecurityContext.
     *
     * @return user profile response
     */
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new InvalidCredentialsException("No authenticated user session found");
        }

        User user = (User) authentication.getPrincipal();
        return userMapper.toResponse(user);
    }

    /**
     * Retrieves user details by UUID for service integrations.
     *
     * @param id user ID
     * @return UserResponse profile
     */
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.cryptovault.auth.exception.ResourceNotFoundException("User not found with ID: " + id));
        return userMapper.toResponse(user);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            else if (Character.isLowerCase(c)) hasLowercase = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }

        if (!hasUppercase || !hasLowercase || !hasDigit) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
            );
        }
    }
}
