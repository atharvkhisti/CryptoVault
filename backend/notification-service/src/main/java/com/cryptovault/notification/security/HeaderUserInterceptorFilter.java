package com.cryptovault.notification.security;

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
 * <p><b>Why it exists:</b> Resolves authenticated user identity context directly from headers propagated by the API Gateway.</p>
 * <p><b>Architectural Layer:</b> Security / Filter Layer.</p>
 * <p><b>Design Patterns Used:</b> Interceptor / Filter Chain Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Trust boundary validation, direct header mapping, stateless context instantiation.</p>
 * <p><b>Enterprise Relevance:</b> Downstream microservices don't repeat the cryptographic parsing of JWTs. They read standard HTTP headers injected at the Gateway perimeter, maximizing horizontal scalability.</p>
 * <p><b>Interview Talking Points:</b> Extracts <code>X-USER-ID</code> (UUID), <code>X-USER-EMAIL</code>, and <code>X-USER-ROLE</code> from headers, wraps them in {@link JwtUserPrincipal}, and populates Spring's {@link SecurityContextHolder}.</p>
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
                // If user UUID is malformed, clear or ignore, allowing security constraints to evaluate request
            }
        }

        filterChain.doFilter(request, response);
    }
}
