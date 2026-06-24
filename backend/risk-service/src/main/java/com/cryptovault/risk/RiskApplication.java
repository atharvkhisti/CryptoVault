package com.cryptovault.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <h3>RiskApplication</h3>
 *
 * <p><b>Why it exists:</b> Entry point and bootstrapping class for the Risk Service microservice.</p>
 * <p><b>Architectural Layer:</b> Bootstrapping / Application Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder and configuration classes auto-bootstrapping.</p>
 * <p><b>Banking Relevance:</b> Centralizes the JVM startup routine of the Risk Assessment and Fraud Mitigation context.</p>
 * <p><b>Scalability Considerations:</b> Enables horizontal scaling as a stateless service that acts strictly on incoming metadata and event logs.</p>
 * <p><b>Interview Talking Points:</b> Uses standard Spring Boot annotations (<code>@SpringBootApplication</code>, <code>@EnableFeignClients</code>) to compile package scans, auto-injecting Feign clients.</p>
 */
@SpringBootApplication
@EnableFeignClients
public class RiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskApplication.class, args);
    }
}
