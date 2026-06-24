package com.cryptovault.gateway.filter;

import com.cryptovault.gateway.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * <h3>GatewayAuthenticationFilter</h3>
 *
 * <p><b>Why it exists:</b> Intercepts all inbound HTTP requests at the gateway boundary to parse, validate JWT tokens, and inject user identity context headers.</p>
 * <p><b>Architectural Layer:</b> Security / Filter Layer.</p>
 * <p><b>Design Patterns Used:</b> Decorator Pattern (via {@link HttpServletRequestWrapper}) and Filter Chain Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Token extraction, perimeter authorization, context wrapper propagation, identity token validation.</p>
 * <p><b>Enterprise Relevance:</b> Enforces microservice perimeter security. Rather than passing raw JWT tokens downstream, it translates the token into verified, trusted headers (X-USER-ID, X-USER-EMAIL, X-USER-ROLE) for downstream microservices, isolating the decryption overhead.</p>
 * <p><b>Interview Talking Points:</b> We use {@link HttpServletRequestWrapper} to mutate the inbound request headers before forwarding. Downstream microservices can trust these headers implicitly because the gateway sits on the network boundary and strips out external custom headers (trust boundaries).</p>
 */
@Component
@RequiredArgsConstructor
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip token validation for public routes
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No JWT token provided. Continue chain so Spring Security can enforce authorization rules.
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            sendUnauthorizedError(response, "Invalid or expired JWT token");
            return;
        }

        String email = jwtService.extractEmail(token);
        String userId = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Decorate the servlet request to inject propagated context headers
        HeaderRequestWrapper wrappedRequest = new HeaderRequestWrapper(request);
        wrappedRequest.addHeader("X-USER-ID", userId != null ? userId : "");
        wrappedRequest.addHeader("X-USER-EMAIL", email != null ? email : "");
        wrappedRequest.addHeader("X-USER-ROLE", role != null ? role : "");

        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.stream().anyMatch(path::equals)) {
            return true;
        }
        return path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/swagger-ui.html") ||
               (path.startsWith("/api/") && path.endsWith("/v3/api-docs"));
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"success\":false,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                message, java.time.LocalDateTime.now()
        ));
    }

    /**
     * Request wrapper decorating the HttpServletRequest to allow runtime header injection.
     */
    private static class HeaderRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new HashMap<>();

        public HeaderRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            this.customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = customHeaders.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> baseNames = super.getHeaderNames();
            while (baseNames.hasMoreElements()) {
                names.add(baseNames.nextElement());
            }
            names.addAll(customHeaders.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String headerValue = customHeaders.get(name);
            if (headerValue != null) {
                return Collections.enumeration(List.of(headerValue));
            }
            return super.getHeaders(name);
        }
    }
}
