package com.cryptovault.notification.dto.response;

import com.cryptovault.common.enums.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>NotificationResponse</h3>
 *
 * <p><b>Why it exists:</b> Data payload returned to consumers indicating tracking records and transmission outcomes for processed alerts.</p>
 * <p><b>Architectural Layer:</b> Data Transfer Object / Response Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Data encapsulation (shields internal ORM entities and database metadata columns from client views).</p>
 * <p><b>Future AWS Integration Path:</b> Acts as the response metadata payload when logging async notification completions.</p>
 * <p><b>Enterprise Relevance:</b> Enforces standardized APIs matching unified rest API design rules.</p>
 * <p><b>Interview Talking Points:</b> Maps database keys and delivery outcomes (like <code>NotificationStatus</code>) alongside transmission timestamps without exposing Hibernate lifecycle properties.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification status tracking response details representation")
public class NotificationResponse {

    @Schema(description = "Unique identifier of the logged notification record", example = "a2bf8a59-122e-407b-a1bc-cd14c2b9a799")
    private UUID notificationId;

    @Schema(description = "Status of dispatch outcome (SENT, FAILED)", example = "SENT")
    private NotificationStatus status;

    @Schema(description = "Timestamp when the notification alert was dispatched", example = "2026-06-19T19:03:49")
    private LocalDateTime sentAt;
}
