package com.cryptovault.kyc.entity;

import com.cryptovault.common.enums.KycStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>KycRecord</h3>
 *
 * <p><b>Why it exists:</b> JPA entity representing a user's compliance/identity verification record.</p>
 * <p><b>Architectural Layer:</b> Persistence Layer / Entity.</p>
 * <p><b>Design Patterns Used:</b> Active Record / Table Mapping Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Acts as the primary audit record representing the customer's clearance to execute financial operations.</p>
 * <p><b>Scalability Considerations:</b> Leverages indexes on user_id to ensure fast status evaluations during wallet transactions.</p>
 * <p><b>Interview Talking Points:</b> Integrates Spring Data JPA Auditing via <code>AuditingEntityListener</code> for automated timestamp generation.</p>
 */
@Entity
@Table(name = "kyc_records", indexes = {
    @Index(name = "idx_kyc_records_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status;

    @Column(length = 1000)
    private String remarks;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
