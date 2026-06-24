package com.cryptovault.audit.entity;

import com.cryptovault.common.enums.AuditEventType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>AuditLog</h3>
 *
 * <p><b>Why it exists:</b> Represents the persisted immutable audit record of a security, authentication, kyc, or financial transaction event.</p>
 * <p><b>Architectural Layer:</b> Persistence / Entity Layer.</p>
 * <p><b>Compliance Relevance:</b> Provides the source-of-truth database record needed for security investigations, GDPR user access audits, and financial reporting compliance (SOX).</p>
 * <p><b>Event-Driven Integration Path:</b> Mapped directly from inbound transaction/wallet events consumed from queues like AWS SQS to log operations asynchronously.</p>
 * <p><b>Enterprise Patterns Used:</b> Active Record / Table Gateway Entity, Immutability Pattern, and Builder Pattern.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Primary key uses UUID to prevent ID enumeration.
 * 2. Immutability is enforced at the JPA layer by omitting setters and registering <code>@PreUpdate</code> / <code>@PreRemove</code> listeners that throw exceptions.
 * 3. Enums are mapped as strings (<code>EnumType.STRING</code>) so database schemas remain readable and resilient to ordinal changes.</p>
 */
@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private AuditEventType eventType;

    @Column(name = "service_name", nullable = false, updatable = false)
    private String serviceName;

    @Column(name = "action", nullable = false, updatable = false)
    private String action;

    @Column(name = "description", nullable = false, updatable = false, length = 1000)
    private String description;

    @Column(name = "ip_address", nullable = false, updatable = false)
    private String ipAddress;

    @Column(name = "performed_by", nullable = false, updatable = false)
    private String performedBy;

    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enforces database-level immutability at the JPA persistence context boundary.
     * Prevents any update action on the persisted record.
     */
    @PreUpdate
    public void preventUpdate() {
        throw new UnsupportedOperationException("Audit log records are immutable and cannot be updated.");
    }

    /**
     * Enforces database-level immutability at the JPA persistence context boundary.
     * Prevents any delete action on the persisted record.
     */
    @PreRemove
    public void preventDelete() {
        throw new UnsupportedOperationException("Audit log records are immutable and cannot be deleted.");
    }
}
