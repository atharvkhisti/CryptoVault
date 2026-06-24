package com.cryptovault.risk.config;

import com.cryptovault.risk.security.HeaderUserInterceptorFilter;
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
 * <p><b>Why it exists:</b> Configures Spring Security authorization filters and endpoints rules.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder pattern for HttpSecurity filter definitions.</p>
 * <p><b>Banking Relevance:</b> Secures endpoint routing contexts, permitting internal evaluations while protecting sensitive compliance history lookups.</p>
 * <p><b>Scalability Considerations:</b> Enforces stateless session management policy, avoiding sticky sessions dependencies.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Permits internal POST requests to <code>/api/risk/evaluate</code> while requiring authentication for historical query paths.
 * 2. Adds the <code>HeaderUserInterceptorFilter</code> before Spring's username password auth filter to bootstrap authenticated principals.</p>
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
                        .requestMatchers(HttpMethod.POST, "/api/risk/evaluate").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/risk/v3/api-docs").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(headerUserInterceptorFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
