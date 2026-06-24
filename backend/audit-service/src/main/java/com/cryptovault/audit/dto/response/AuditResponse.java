package com.cryptovault.audit.dto.response;

import com.cryptovault.common.enums.AuditEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>AuditResponse</h3>
 *
 * <p><b>Why it exists:</b> Payload DTO returned after successfully logging or querying an audit trail record.</p>
 * <p><b>Architectural Layer:</b> DTO / Interface Layer.</p>
 * <p><b>Compliance Relevance:</b> Returns unified metadata summarizing audit logs without leaking excessive internal database columns or binary hashes.</p>
 * <p><b>Event-Driven Integration Path:</b> Provided to monitoring and analytics dashboards tracking live audit event executions.</p>
 * <p><b>Enterprise Patterns Used:</b> Data Transfer Object (DTO) Pattern.</p>
 * <p><b>Interview Talking Points:</b> Maps database structures to a slim JSON schema returned in successful envelopes, isolating database representation from network contract interfaces.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit trail tracking response details representation")
public class AuditResponse {

    @Schema(description = "Unique UUID of the audit log record", example = "c2bf8a59-122e-407b-a1bc-cd14c2b9a800")
    private UUID auditId;

    @Schema(description = "Classification category of the audit event", example = "USER_LOGIN")
    private AuditEventType eventType;

    @Schema(description = "Functional action executed within the platform boundary", example = "USER_LOGIN_SUCCESS")
    private String action;

    @Schema(description = "Timestamp when the audit event occurred", example = "2026-06-19T19:03:49")
    private LocalDateTime eventTimestamp;
}
