package com.cryptovault.gateway.config;

import com.cryptovault.gateway.filter.GatewayAuthenticationFilter;
import com.cryptovault.gateway.filter.LoggingFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * <h3>SecurityConfig</h3>
 *
 * <p><b>Why it exists:</b> Centralizes and sets up web security layers at the API gateway entry point.</p>
 * <p><b>Architectural Layer:</b> Configuration / Security Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern (for HttpSecurity) and Strategy Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Stateless JWT authentication, CORS policies, CSRF exclusion, session policy containment.</p>
 * <p><b>Enterprise Relevance:</b> Enforces client communication protocols at the perimeter before traffic routes internally, defending against cross-site scripting/request forgery vectors.</p>
 * <p><b>Interview Talking Points:</b> Serves as a stateless security context. Since we use JWT authorization, we disable sessions ({@link SessionCreationPolicy#STATELESS}) and CSRF. Our custom filter runs before {@link UsernamePasswordAuthenticationFilter} to inject downstream headers.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthenticationFilter authenticationFilter;
    private final LoggingFilter loggingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/api/*/v3/api-docs", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(String.format(
                        "{\"success\":false,\"message\":\"Unauthorized: Missing or invalid token\",\"data\":null,\"timestamp\":\"%s\"}",
                        java.time.LocalDateTime.now()
                    ));
                })
            )
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(loggingFilter, GatewayAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-USER-ID", "X-USER-EMAIL", "X-USER-ROLE"));
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
