package com.cryptovault.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * <h3>JpaAuditingConfig</h3>
 *
 * <p><b>Why it exists:</b> Enables JPA auditing to automate creation timestamp capture for domain entities.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Bootstrapped Configuration Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Enforces timestamp integrity at the persistence level.</p>
 * <p><b>Future AWS Integration Path:</b> Generates timestamps when mapping SQS records to database records.</p>
 * <p><b>Enterprise Relevance:</b> Enforces chronological integrity across platform data history audits.</p>
 * <p><b>Interview Talking Points:</b> Modularity in configurations. Keeping auditing separate from the main class is clean and helps in slice tests.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
