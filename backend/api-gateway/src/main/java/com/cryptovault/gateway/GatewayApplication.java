package com.cryptovault.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <h3>GatewayApplication</h3>
 *
 * <p><b>Why it exists:</b> Serves as the bootstrap entry point for the CryptoVault API Gateway microservice.</p>
 * <p><b>Architectural Layer:</b> Bootstrap/Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Singleton (via Spring's ApplicationContext) and Facade (bootstraps auto-configuration).</p>
 * <p><b>Security Concepts Demonstrated:</b> Acts as the boundary layer protecting downstream services.</p>
 * <p><b>Enterprise Relevance:</b> Bootstrapping class for Spring Cloud Gateway MVC (Servlet-based), configured to route all traffic to other services.</p>
 * <p><b>Interview Talking Points:</b> Serves as a single ingress node. We use the servlet-based Gateway Server MVC (Spring Boot 3.x / Spring Cloud 2025.x) instead of reactive WebFlux when the ecosystem relies on synchronous WebMVC blocks and standard thread pools, aligning with our thread-safety configurations.</p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
