package com.cryptovault.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service class for constructing, parsing, and validating JSON Web Tokens (JWT).
 */
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key:Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA==}")
    private String secretKey;

    @Value("${application.security.jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    /**
     * Extracts the username (subject) claim from the token.
     *
     * @param token the JWT token
     * @return the extracted username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a single custom claim from the token using a claims resolver function.
     *
     * @param token          the JWT token
     * @param claimsResolver functional interface resolver
     * @param <T>            the resolved type
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a signed JWT access token for the authenticated user.
     *
     * @param userDetails the user context profile
     * @return the compacted JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof com.cryptovault.auth.entity.User) {
            com.cryptovault.auth.entity.User user = (com.cryptovault.auth.entity.User) userDetails;
            extraClaims.put("userId", user.getId() != null ? user.getId().toString() : "");
            extraClaims.put("role", user.getRole() != null ? user.getRole().name() : "");
            extraClaims.put("email", user.getEmail() != null ? user.getEmail() : "");
        }
        return generateToken(extraClaims, userDetails);
    }

    /**
     * Generates a signed JWT access token containing additional claims payload.
     *
     * @param extraClaims map containing custom claims to inject
     * @param userDetails the user context profile
     * @return the compacted JWT token string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Gets token validity expiration duration.
     *
     * @return expiration time in milliseconds
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Checks token signature integrity and temporal validity against user context.
     *
     * @param token       the JWT token string
     * @param userDetails user details
     * @return true if valid and not expired, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
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
