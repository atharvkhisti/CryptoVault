package com.cryptovault.notification.entity;

import com.cryptovault.common.enums.NotificationStatus;
import com.cryptovault.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>Notification</h3>
 *
 * <p><b>Why it exists:</b> Represents the persisted record of alerts, tracking history and statuses of sent emails in the PostgreSQL database.</p>
 * <p><b>Architectural Layer:</b> Persistence / Domain Layer.</p>
 * <p><b>Design Patterns Used:</b> Anemic Domain Model / Entity Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Preserves a read-only historical record of user communications for security auditing and forensic reviews.</p>
 * <p><b>Future AWS Integration Path:</b> When messages are consumed from SQS, this entity is created and populated with transaction details before email delivery.</p>
 * <p><b>Enterprise Relevance:</b> Enforces audit transparency by keeping a history of customer notifications (such as transfers or risk alerts).</p>
 * <p><b>Interview Talking Points:</b> Implements {@link AuditingEntityListener} to automatically set creation timestamps. Enums are mapped as strings (<code>EnumType.STRING</code>) to protect the schema if enums are reordered or extended.</p>
 */
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
