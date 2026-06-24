package com.cryptovault.risk.dto.response;

import com.cryptovault.common.enums.AuditEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>AuditResponse</h3>
 *
 * <p><b>Why it exists:</b> Maps JSON response payloads representing compliance logs from the Audit Service.</p>
 * <p><b>Architectural Layer:</b> DTO / Integration Layer.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponse {
    private UUID auditId;
    private AuditEventType eventType;
    private String action;
    private LocalDateTime eventTimestamp;
}
