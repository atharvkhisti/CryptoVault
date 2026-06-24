package com.cryptovault.auth.controller;

import com.cryptovault.auth.dto.request.LoginRequest;
import com.cryptovault.auth.dto.request.RegisterRequest;
import com.cryptovault.auth.dto.response.AuthResponse;
import com.cryptovault.auth.dto.response.UserResponse;
import com.cryptovault.auth.service.AuthService;
import com.cryptovault.auth.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller mapping incoming API calls to /api/auth.
 * Exposes login, registration, and session profile endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Interface", description = "Endpoints for user registration, authentication, and session profiling")
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint to register a new user in the platform.
     *
     * @param request register body payload
     * @return 201 CREATED status with profile payload
     */
    @PostMapping("/register")
    @Operation(summary = "Register User", description = "Registers a new user in the platform database.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameter failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email address already registered conflict", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success("User registered successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Endpoint to authenticate users.
     *
     * @param request login payload credentials
     * @return 200 OK status containing JWT token payload
     */
    @PostMapping("/login")
    @Operation(summary = "Login / Authenticate User", description = "Authenticates user credentials, returning a signed stateless JWT token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials provided", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    /**
     * Endpoint to retrieve details of the currently authenticated user session.
     *
     * @return 200 OK containing user info DTO
     */
    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Retrieves profile information associated with the caller's JWT token context.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user profile retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved successfully", response));
    }

    /**
     * Endpoint to retrieve details of a specific user by UUID for service integration.
     *
     * @param id the user ID
     * @return 200 OK containing user info DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get User Profile by ID", description = "Internal service lookup querying profile details of a specific user by UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable("id") @Parameter(description = "Unique UUID of the user", example = "123e4567-e89b-12d3-a456-426614174000") UUID id
    ) {
        UserResponse response = authService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }
}
