package com.cryptovault.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * <h3>JwtService</h3>
 *
 * <p><b>Why it exists:</b> Validates JSON Web Tokens locally, decoding properties and validating cryptographic signatures at the perimeter.</p>
 * <p><b>Architectural Layer:</b> Infrastructure / Security Layer.</p>
 * <p><b>Design Patterns Used:</b> Stateless Strategy / Claim Resolver helper.</p>
 * <p><b>Security Concepts Demonstrated:</b> Local cryptographic signature verification, temporal validation, claims parsing.</p>
 * <p><b>Enterprise Relevance:</b> Crucial for avoiding back-and-forth network requests between services (the "chatty" microservice anti-pattern) during authentication.</p>
 * <p><b>Interview Talking Points:</b> Local token decoding is done via symmetric signing keys shared out-of-band (via application configuration). The gateway validates the token locally and extracts identity contexts without hitting databases.</p>
 */
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    /**
     * Extracts subject (username / email) from token claims.
     *
     * @param token JWT token string
     * @return the subject of the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts user ID from token claims.
     *
     * @param token JWT token string
     * @return the userId claim value
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extracts user role from token claims.
     *
     * @param token JWT token string
     * @return the role claim value
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extracts user email from token claims.
     *
     * @param token JWT token string
     * @return the email claim value
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Helper resolver to extract specific claims from the token using a functional strategy.
     *
     * @param token          JWT token string
     * @param claimsResolver claim mapping logic
     * @param <T>            the return type of mapped claim
     * @return extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates token integrity and ensures the token has not expired.
     *
     * @param token JWT token string
     * @return true if token is valid and unexpired
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
