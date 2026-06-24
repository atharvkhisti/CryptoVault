package com.cryptovault.kyc.config;

import com.cryptovault.kyc.security.HeaderUserInterceptorFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h3>SecurityConfig</h3>
 *
 * <p><b>Why it exists:</b> Configures Spring Security authorization filters and endpoints rules.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder pattern for HttpSecurity filter definitions.</p>
 * <p><b>Financial Compliance Relevance:</b> Restricts critical KYC approval and rejection endpoints to administrators, maintaining segregation of duties.</p>
 * <p><b>Scalability Considerations:</b> Enforces stateless session management policy, avoiding sticky session requirements.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Permits actuator health checks and metrics endpoint while protecting KYC operations.
 * 2. Restricts <code>/api/kyc/approve</code> and <code>/api/kyc/reject</code> to users with ADMIN roles.
 * 3. Integrates the <code>HeaderUserInterceptorFilter</code> to extract Gateway contexts on every request.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderUserInterceptorFilter headerUserInterceptorFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/kyc/v3/api-docs").permitAll()
                        .requestMatchers("/api/kyc/approve", "/api/kyc/reject").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(headerUserInterceptorFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
