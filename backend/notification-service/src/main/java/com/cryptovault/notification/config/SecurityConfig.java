package com.cryptovault.notification.config;

import com.cryptovault.notification.security.HeaderUserInterceptorFilter;
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
 * <p><b>Why it exists:</b> Establishes boundary security rules for the Notification Service, specifying permitted and authenticated HTTP request mapping routes.</p>
 * <p><b>Architectural Layer:</b> Configuration / Security Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern for HttpSecurity configurations.</p>
 * <p><b>Security Concepts Demonstrated:</b> Stateless filter chain configuration, endpoint permissions, internal/external interface partitioning.</p>
 * <p><b>Enterprise Relevance:</b> Secures transaction logs. Permits service-to-service notification requests to allow anonymous registrations to trigger alert mails, while protecting access to historic communication lookup APIs.</p>
 * <p><b>Interview Talking Points:</b> Configures <code>SessionCreationPolicy.STATELESS</code> because we rely on external JWT checks, injecting {@link HeaderUserInterceptorFilter} to populate authentication context before Spring Security filters run.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderUserInterceptorFilter headerUserInterceptorFilter;

    /**
     * Security filter chain definition.
     *
     * @param http the security builder
     * @return filter chain bean
     * @throws Exception configuration exceptions
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/notifications/send").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/notifications/v3/api-docs").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(headerUserInterceptorFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
