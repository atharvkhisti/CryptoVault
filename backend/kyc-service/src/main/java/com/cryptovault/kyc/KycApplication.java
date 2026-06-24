package com.cryptovault.kyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <h3>KycApplication</h3>
 *
 * <p><b>Why it exists:</b> Microservice bootstrapper that configures and launches the Spring Boot runtime context for the KYC Service.</p>
 * <p><b>Architectural Layer:</b> Application Entrypoint Layer.</p>
 * <p><b>Design Patterns Used:</b> Bootstrapping / Application Registry Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Ensures the service registers and initializes all context configs correctly, providing core identity checking functionality.</p>
 * <p><b>Scalability Considerations:</b> Enables microservices architecture, allowing the KYC service to scale independently from transaction and wallet services.</p>
 * <p><b>Interview Talking Points:</b> Configures openfeign client discovery using `@EnableFeignClients`, allowing discovery-free inter-service communication contracts.</p>
 */
@SpringBootApplication
@EnableFeignClients
public class KycApplication {

    public static void main(String[] args) {
        SpringApplication.run(KycApplication.class, args);
    }
}
