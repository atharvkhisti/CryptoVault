package com.cryptovault.audit.config;

import com.cryptovault.audit.security.HeaderUserInterceptorFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h3>SecurityConfig</h3>
 *
 * <p><b>Why it exists:</b> Sets up path authorizations, permitting incoming internal microservices to log events, while requiring verified user contexts for queries.</p>
 * <p><b>Architectural Layer:</b> Configuration / Security Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern for HttpSecurity configuration.</p>
 * <p><b>Security Concepts Demonstrated:</b> Service-to-service trust boundaries, path-based access control, stateless session verification.</p>
 * <p><b>Enterprise Relevance:</b> Prevents public clients from bypassing gateway rules to query compliance logs directly, while allowing microservice contexts to securely log audit trails.</p>
 * <p><b>Interview Talking Points:</b> Integrates <code>SessionCreationPolicy.STATELESS</code> and overrides CSRF bounds, adding the custom {@link HeaderUserInterceptorFilter} to map Gateway user contexts before Spring Security chains run.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderUserInterceptorFilter headerUserInterceptorFilter;

    /**
     * Configures route rules and request filter interceptors.
     *
     * @param http the HttpSecurity builder
     * @return the build security filter chain
     * @throws Exception configuration exceptions
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/audit").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/audit/v3/api-docs").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(headerUserInterceptorFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
