package com.cryptovault.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * <h3>GatewayConfig</h3>
 *
 * <p><b>Why it exists:</b> Programmatically configures routing mappings that translate external client URI pathing (e.g. /api/wallets/**) to physical downstream microservice URLs.</p>
 * <p><b>Architectural Layer:</b> Configuration / Routing Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern and Routing Table Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Service isolation and ingress filtering boundaries (routes are only bound to designated API prefixes).</p>
 * <p><b>Enterprise Relevance:</b> Abstracts service location details from the clients. Permits cloud infrastructure teams to transparently reposition, auto-scale, or redesign internal service ports without modifying consumer-facing application clients.</p>
 * <p><b>Interview Talking Points:</b> The routes are configured using servlet-based {@link RouterFunction} beans rather than the reactive WebFlux routes. Downstream service URLs are injected using Spring's property replacement value annotations, allowing environment overrides for staging, QA, and production. In modern Spring Cloud Gateway Server MVC, we use parameterless <code>http()</code> combined with the <code>before(uri(URI))</code> filter function to specify backend destinations.</p>
 */
@Configuration
public class GatewayConfig {

    @Value("${services.auth.url}")
    private String authServiceUrl;

    @Value("${services.wallet.url}")
    private String walletServiceUrl;

    @Value("${services.transaction.url}")
    private String transactionServiceUrl;

    @Value("${services.notification.url}")
    private String notificationServiceUrl;

    @Value("${services.risk.url}")
    private String riskServiceUrl;

    @Value("${services.audit.url}")
    private String auditServiceUrl;

    @Value("${services.kyc.url}")
    private String kycServiceUrl;

    /**
     * Auth Service routing mapping.
     */
    @Bean
    public RouterFunction<ServerResponse> authRoute() {
        return route("auth-service")
                .route(RequestPredicates.path("/api/auth/**"), http())
                .before(uri(URI.create(authServiceUrl)))
                .build();
    }

    /**
     * Wallet Service routing mapping.
     */
    @Bean
    public RouterFunction<ServerResponse> walletRoute() {
        return route("wallet-service")
                .route(RequestPredicates.path("/api/wallets/**"), http())
                .before(uri(URI.create(walletServiceUrl)))
                .build();
    }

    /**
     * Transaction Service routing mapping.
     */
    @Bean
    public RouterFunction<ServerResponse> transactionRoute() {
        return route("transaction-service")
                .route(RequestPredicates.path("/api/transactions/**"), http())
                .before(uri(URI.create(transactionServiceUrl)))
                .build();
    }

    /**
     * Notification Service routing placeholder.
     */
    @Bean
    public RouterFunction<ServerResponse> notificationRoute() {
        return route("notification-service")
                .route(RequestPredicates.path("/api/notifications/**"), http())
                .before(uri(URI.create(notificationServiceUrl)))
                .build();
    }

    /**
     * Risk Service routing placeholder.
     */
    @Bean
    public RouterFunction<ServerResponse> riskRoute() {
        return route("risk-service")
                .route(RequestPredicates.path("/api/risk/**"), http())
                .before(uri(URI.create(riskServiceUrl)))
                .build();
    }

    /**
     * Audit Service routing placeholder.
     */
    @Bean
    public RouterFunction<ServerResponse> auditRoute() {
        return route("audit-service")
                .route(RequestPredicates.path("/api/audit/**"), http())
                .before(uri(URI.create(auditServiceUrl)))
                .build();
    }

    /**
     * KYC Service routing placeholder.
     */
    @Bean
    public RouterFunction<ServerResponse> kycRoute() {
        return route("kyc-service")
                .route(RequestPredicates.path("/api/kyc/**"), http())
                .before(uri(URI.create(kycServiceUrl)))
                .build();
    }
}
