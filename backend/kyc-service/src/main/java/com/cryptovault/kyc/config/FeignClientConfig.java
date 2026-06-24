package com.cryptovault.kyc.config;

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
 * <p><b>Financial Compliance Relevance:</b> Propagates critical security identity fields (userId, email, roles) through Feign calls, maintaining the audit trail across services.</p>
 * <p><b>Scalability Considerations:</b> Extremely low overhead since it copies headers from the active thread context.</p>
 * <p><b>Interview Talking Points:</b> Pulls headers from the incoming servlet thread context via <code>RequestContextHolder</code> and replicates them onto outbound Feign request templates.</p>
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
                    
                    // Propagate Gateway identity headers
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
