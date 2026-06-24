package com.cryptovault.risk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * <h3>JpaAuditingConfig</h3>
 *
 * <p><b>Why it exists:</b> Configuration to bootstrap Spring Data JPA auditing capabilities.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Configuration adapter pattern.</p>
 * <p><b>Banking Relevance:</b> Ensures chronological data modification fields like created_at are automatically and reliably managed.</p>
 * <p><b>Scalability Considerations:</b> Handled in-memory locally, avoiding any additional queries or lock cycles.</p>
 * <p><b>Interview Talking Points:</b> Annotating this class with <code>@EnableJpaAuditing</code> enables entity listeners to automatically capture auditing events.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
