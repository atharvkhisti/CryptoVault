package com.cryptovault.kyc.security;

import com.cryptovault.common.security.JwtUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * <h3>HeaderUserInterceptorFilter</h3>
 *
 * <p><b>Why it exists:</b> Decodes user context variables from gateway-mutated requests and registers the principal.</p>
 * <p><b>Architectural Layer:</b> Security / Filter Layer.</p>
 * <p><b>Design Patterns Used:</b> Interceptor Filter Chain Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Propagates identity context across downstream calls, keeping compliance logs populated with verified user references.</p>
 * <p><b>Scalability Considerations:</b> Highly scalable as context construction runs in-memory with zero database lookups.</p>
 * <p><b>Interview Talking Points:</b> Extracts Gateway-injected headers (X-USER-ID, X-USER-EMAIL, X-USER-ROLE) to instantiate <code>JwtUserPrincipal</code>, establishing Spring Security context dynamically.</p>
 */
@Component
public class HeaderUserInterceptorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String userIdStr = request.getHeader("X-USER-ID");
        String email = request.getHeader("X-USER-EMAIL");
        String role = request.getHeader("X-USER-ROLE");

        if (userIdStr != null && !userIdStr.trim().isEmpty() && email != null && role != null) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                JwtUserPrincipal principal = JwtUserPrincipal.builder()
                        .userId(userId)
                        .email(email)
                        .role(role)
                        .build();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (IllegalArgumentException e) {
                // Ignore malformed UUID
            }
        }

        filterChain.doFilter(request, response);
    }
}
