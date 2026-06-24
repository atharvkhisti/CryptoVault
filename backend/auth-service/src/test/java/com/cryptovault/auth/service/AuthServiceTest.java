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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests validating core business flow logic in {@link AuthService}.
 * Covers user registration, password strength policy enforcement, user login context loading,
 * authentication failures, and session extraction.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_ShouldRegisterUserSuccessfully_WhenDataIsValid() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@cryptovault.com")
                .password("StrongPass123")
                .build();

        User user = User.builder()
                .name("Alice")
                .email("alice@cryptovault.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@cryptovault.com")
                .role(Role.USER)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals("Alice", response.getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@cryptovault.com")
                .password("StrongPass123")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenPasswordIsWeak() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@cryptovault.com")
                .password("weak")
                .build();

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldAuthenticateAndReturnTokens_WhenCredentialsAreCorrect() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@cryptovault.com")
                .password("StrongPass123")
                .build();

        User user = User.builder()
                .name("Alice")
                .email("alice@cryptovault.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(user)).thenReturn("jwt_access_token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt_access_token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());
    }

    @Test
    void login_ShouldThrowException_WhenCredentialsAreIncorrect() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@cryptovault.com")
                .password("wrong")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void getCurrentUser_ShouldReturnProfile_WhenAuthenticated() {
        User user = User.builder()
                .name("Alice")
                .email("alice@cryptovault.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@cryptovault.com")
                .role(Role.USER)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse response = authService.getCurrentUser();

        assertNotNull(response);
        assertEquals("alice@cryptovault.com", response.getEmail());
    }

    @Test
    void getCurrentUser_ShouldThrowException_WhenNoAuthenticationSession() {
        assertThrows(InvalidCredentialsException.class, () -> authService.getCurrentUser());
    }
}
