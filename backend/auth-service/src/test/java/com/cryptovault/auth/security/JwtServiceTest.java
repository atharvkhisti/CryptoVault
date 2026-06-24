package com.cryptovault.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link JwtService} verifying claims extraction,
 * token signatures, validation rules, and lifecycle expiration logic.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject values using Spring's ReflectionTestUtils to mimic Spring @Value properties injection
        ReflectionTestUtils.setField(jwtService, "secretKey", "Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA==");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour

        userDetails = User.builder()
                .username("test@cryptovault.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertEquals("test@cryptovault.com", jwtService.extractUsername(token));
    }

    @Test
    void shouldExtractUsernameCorrectly() {
        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);
        assertEquals("test@cryptovault.com", username);
    }

    @Test
    void shouldReturnTokenExpirationTime() {
        assertEquals(3600000L, jwtService.getExpirationTime());
    }

    @Test
    void shouldFailValidationIfUsernameDoesNotMatch() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUserDetails = User.builder()
                .username("other@cryptovault.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        assertFalse(jwtService.isTokenValid(token, otherUserDetails));
    }
}
