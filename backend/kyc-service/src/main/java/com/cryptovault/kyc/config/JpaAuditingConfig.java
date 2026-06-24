package com.cryptovault.kyc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * <h3>JpaAuditingConfig</h3>
 *
 * <p><b>Why it exists:</b> Enables automatic population of JPA auditing fields like {@code @CreatedDate} and {@code @LastModifiedDate}.</p>
 * <p><b>Architectural Layer:</b> Infrastructure Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Callback Auditing Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Guarantees immutable tracking of document submission times and compliance review dates, vital for security audits.</p>
 * <p><b>Scalability Considerations:</b> Offloads auditable date generation to hibernate lifecycle loops, saving extra database calls.</p>
 * <p><b>Interview Talking Points:</b> Decouples database auditing fields from business REST service handlers, guaranteeing standard date management.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
