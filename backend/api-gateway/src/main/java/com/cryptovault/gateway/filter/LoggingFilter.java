package com.cryptovault.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <h3>LoggingFilter</h3>
 *
 * <p><b>Why it exists:</b> Intercepts all gateway requests to audit response statuses, API path executions, and trace latencies.</p>
 * <p><b>Architectural Layer:</b> Cross-Cutting Concerns / Observability Layer.</p>
 * <p><b>Design Patterns Used:</b> Interceptor / Filter Chain Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Complete audit logging of edge entry points for forensic security traceabilities.</p>
 * <p><b>Enterprise Relevance:</b> Provides structured metrics suitable for log aggregation tools (Splunk, ELK, Prometheus, Grafana Loki) to generate charts, calculate p99 response times, and alert on error rate spikes.</p>
 * <p><b>Interview Talking Points:</b> In microservice architectures, tracking request lifecycles at the entry point is vital. The logging is completed in a <code>finally</code> block to ensure request latency is measured accurately regardless of execution errors. It pulls identity context from {@link SecurityContextHolder}.</p>
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String username = getAuthenticatedUser();

            // Structured logging prefix for log shippers to easily scrape metrics (Prometheus/Grafana ecosystem)
            log.info("GATEWAY_METRIC | method={} | path={} | status={} | duration_ms={} | user={}",
                    method, path, status, duration, username);
        }
    }

    private String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
