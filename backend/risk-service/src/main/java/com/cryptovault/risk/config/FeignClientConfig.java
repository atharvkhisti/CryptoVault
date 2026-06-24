package com.cryptovault.risk.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <h3>FeignClientConfig</h3>
 *
 * <p><b>Why it exists:</b> Contextually configures Feign interceptor behaviors to copy security headers downstream.</p>
 * <p><b>Architectural Layer:</b> Configuration / Integration Layer.</p>
 * <p><b>Design Patterns Used:</b> Interceptor pattern for out-of-band header copies.</p>
 * <p><b>Banking Relevance:</b> Secures inter-service REST transactions by carrying authorization states across microservices hops.</p>
 * <p><b>Scalability Considerations:</b> Extremely cheap operation using local context headers mapping.</p>
 * <p><b>Interview Talking Points:</b> Pulls the active authorization token using Spring Web's <code>RequestContextHolder</code> and appends it to outbox templates.</p>
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authorizationHeader = request.getHeader("Authorization");
                    if (authorizationHeader != null && !authorizationHeader.trim().isEmpty()) {
                        template.header("Authorization", authorizationHeader);
                    }
                    
                    // Also propagate Gateway headers if present
                    String userId = request.getHeader("X-USER-ID");
                    String email = request.getHeader("X-USER-EMAIL");
                    String role = request.getHeader("X-USER-ROLE");
                    
                    if (userId != null) template.header("X-USER-ID", userId);
                    if (email != null) template.header("X-USER-EMAIL", email);
                    if (role != null) template.header("X-USER-ROLE", role);
                }
            }
        };
    }
}
