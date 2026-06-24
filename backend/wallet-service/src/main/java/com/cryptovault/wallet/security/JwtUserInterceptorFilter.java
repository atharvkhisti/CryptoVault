package com.cryptovault.wallet.security;

import com.cryptovault.common.security.JwtUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Custom interceptor filter that decodes the JWT access token from incoming HTTP headers,
 * extracts claims, and populates the {@link SecurityContextHolder} with {@link JwtUserPrincipal}.
 * This avoids remote HTTP round-trips to the Auth Service.
 */
@Component
@RequiredArgsConstructor
public class JwtUserInterceptorFilter extends OncePerRequestFilter {

    @Value("${application.security.jwt.secret-key:Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA==}")
    private String secretKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

            String email = claims.getSubject();
            String userIdStr = claims.get("userId", String.class);
            String roleStr = claims.get("role", String.class);

            if (email != null && userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = UUID.fromString(userIdStr);
                JwtUserPrincipal principal = JwtUserPrincipal.builder()
                        .userId(userId)
                        .email(email)
                        .role(roleStr)
                        .build();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + roleStr))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ex) {
            // Token verification failures (expiry, signature mismatch) are swallowed
            // to allow Spring Security config chain to evaluate permissions
        }

        filterChain.doFilter(request, response);
    }
}
