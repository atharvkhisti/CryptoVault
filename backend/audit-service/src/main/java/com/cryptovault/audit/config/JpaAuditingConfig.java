package com.cryptovault.audit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * <h3>JpaAuditingConfig</h3>
 *
 * <p><b>Why it exists:</b> Configures JPA auditing triggers, enabling <code>@CreatedDate</code> and other lifecycle annotations across database entity attributes.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Compliance Relevance:</b> Automates immutable database timestamp captures, preventing manual code from updating creation timestamps, maintaining the reliability of compliance records.</p>
 * <p><b>Event-Driven Integration Path:</b> Automatically records transaction entry timestamps upon event insertion.</p>
 * <p><b>Enterprise Patterns Used:</b> Aspect-Oriented Auditing Pattern.</p>
 * <p><b>Interview Talking Points:</b> Uses <code>@EnableJpaAuditing</code> to instruct Hibernate / Spring Data JPA to observe lifecycle hooks and populate datetime metrics automatically.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
