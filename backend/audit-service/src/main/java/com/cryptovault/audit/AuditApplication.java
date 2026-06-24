package com.cryptovault.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <h3>AuditApplication</h3>
 *
 * <p><b>Why it exists:</b> The main entry point bootstrapping the Spring Boot application configuration and IOC context for the Audit microservice.</p>
 * <p><b>Architectural Layer:</b> Bootstrapping / Application Layer.</p>
 * <p><b>Compliance Relevance:</b> Configures database connection bindings and logging frameworks, which are foundations for GDPR/SOX platform tracking.</p>
 * <p><b>Event-Driven Integration Path:</b> Bootstraps listeners and event queues configurations during lifecycle startup.</p>
 * <p><b>Enterprise Patterns Used:</b> Application Entry Point Pattern.</p>
 * <p><b>Interview Talking Points:</b> Standard Spring Boot bootstrapper utilizing <code>@SpringBootApplication</code>. Scans local context beans, initializing port <code>8086</code> mappings automatically.</p>
 */
@SpringBootApplication
public class AuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
