package com.cryptovault.wallet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class enabling JPA Auditing listeners for automatically managing
 * entity creation and modification audit dates.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
