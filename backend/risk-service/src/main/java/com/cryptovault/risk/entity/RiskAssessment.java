package com.cryptovault.risk.entity;

import com.cryptovault.common.enums.RiskLevel;
import com.cryptovault.common.enums.RiskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>RiskAssessment</h3>
 *
 * <p><b>Why it exists:</b> JPA entity representing persistent records of transaction and user risk evaluations.</p>
 * <p><b>Architectural Layer:</b> Persistence / Entity Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern, Active Record / Table Gateway pattern mapping.</p>
 * <p><b>Banking Relevance:</b> Maintains compliance logs to satisfy regulatory requirements (AML/KYC guidelines) and audit traces of transaction fraud checks.</p>
 * <p><b>Scalability Considerations:</b> Leverages UUID primary keys to prevent ID enumeration and support distributed database sharding by user ID.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Enums are mapped as <code>EnumType.STRING</code> to keep SQL tables readable and resilient to ordinal changes.
 * 2. Uses <code>@EntityListeners(AuditingEntityListener.class)</code> to automate timestamp recording for audit trails.</p>
 */
@Entity
@Table(name = "risk_assessments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RiskStatus status;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "triggered_rule")
    private String triggeredRule;

    @Column(name = "comments", length = 1000)
    private String comments;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
