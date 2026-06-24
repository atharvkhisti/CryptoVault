package com.cryptovault.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h3>JwtServiceTest</h3>
 *
 * <p><b>Why it exists:</b> Validates cryptographic parser logic, signature check operations, and expiration policies in {@link JwtService}.</p>
 * <p><b>Architectural Layer:</b> Testing Layer.</p>
 * <p><b>Design Patterns Used:</b> Test Spy / Injection Mocking.</p>
 * <p><b>Security Concepts Demonstrated:</b> Cryptographic validity assertions, claims extraction tests, and token structure integrity checks.</p>
 * <p><b>Enterprise Relevance:</b> Ensures that token verification functions flawlessly, avoiding security gaps or accidental access denials.</p>
 * <p><b>Interview Talking Points:</b> Tests signature checking using a test key mimicking the real base64-encoded secret key, verifying that claims like email, role, and userId are correctly parsed and returned.</p>
 */
class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
    }

    private String createToken(String subject, Map<String, Object> claims, long expirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldExtractUsernameAndClaimsSuccessfully() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "123e4567-e89b-12d3-a456-426614174000");
        claims.put("role", "ADMIN");
        claims.put("email", "admin@cryptovault.com");

        String token = createToken("admin@cryptovault.com", claims, 60000);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("admin@cryptovault.com", jwtService.extractUsername(token));
        assertEquals("123e4567-e89b-12d3-a456-426614174000", jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals("admin@cryptovault.com", jwtService.extractEmail(token));
    }

    @Test
    void shouldFailValidationWhenTokenExpired() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "user-id");
        claims.put("role", "USER");

        String token = createToken("user@cryptovault.com", claims, -1000); // already expired

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void shouldFailValidationWhenTokenIsInvalid() {
        String invalidToken = "invalid.jwt.token";
        assertFalse(jwtService.isTokenValid(invalidToken));
    }
}
