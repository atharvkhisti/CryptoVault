package com.cryptovault.audit.mapper;

import com.cryptovault.audit.dto.request.AuditEventRequest;
import com.cryptovault.audit.dto.response.AuditResponse;
import com.cryptovault.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * <h3>AuditMapper</h3>
 *
 * <p><b>Why it exists:</b> Performs translation mapping conversions between {@link AuditLog} entities and DTO request/response boundaries.</p>
 * <p><b>Architectural Layer:</b> Mapping / Translation Layer.</p>
 * <p><b>Compliance Relevance:</b> Prevents deep entity mutations by converting data cleanly to stateless DTO models before network serialization.</p>
 * <p><b>Event-Driven Integration Path:</b> Converts event payloads received from AWS SQS into persistent database entity models.</p>
 * <p><b>Enterprise Patterns Used:</b> Translator / Data Mapper Pattern.</p>
 * <p><b>Interview Talking Points:</b> Declared as a Spring <code>@Component</code>. Does not use heavy reflection libraries, mapping fields explicitly via builders to maximize compilation speed and execution efficiency.</p>
 */
@Component
public class AuditMapper {

    /**
     * Translates log event requests to persistent log entity mappings.
     * Note: eventTimestamp is default-assigned to LocalDateTime.now() at execution time.
     * performedBy is dynamically populated by the caller context.
     *
     * @param request the inbound event details DTO
     * @param performedBy the identity performing the operation
     * @return the build AuditLog entity mapping
     */
    public AuditLog toEntity(AuditEventRequest request, String performedBy) {
        if (request == null) {
            return null;
        }
        return AuditLog.builder()
                .userId(request.getUserId())
                .eventType(request.getEventType())
                .serviceName(request.getServiceName())
                .action(request.getAction())
                .description(request.getDescription())
                .ipAddress(request.getIpAddress())
                .performedBy(performedBy)
                .eventTimestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Translates a persisted entity to a slim return DTO response envelope.
     *
     * @param log the persisted log entity
     * @return the AuditResponse mapping details
     */
    public AuditResponse toResponse(AuditLog log) {
        if (log == null) {
            return null;
        }
        return AuditResponse.builder()
                .auditId(log.getId())
                .eventType(log.getEventType())
                .action(log.getAction())
                .eventTimestamp(log.getEventTimestamp())
                .build();
    }
}
