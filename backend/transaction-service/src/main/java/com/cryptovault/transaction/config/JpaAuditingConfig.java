package com.cryptovault.transaction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration that activates JPA auditing fields across domain entities.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
