package com.cryptovault.audit.repository;

import com.cryptovault.common.enums.AuditEventType;
import com.cryptovault.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <h3>AuditLogRepository</h3>
 *
 * <p><b>Why it exists:</b> Manages read/write database queries for retrieving or persisting immutable {@link AuditLog} records.</p>
 * <p><b>Architectural Layer:</b> Persistence / Repository Layer.</p>
 * <p><b>Compliance Relevance:</b> Enables auditors and security engineers to extract historical access and transaction audit trail segments for external/internal reporting.</p>
 * <p><b>Event-Driven Integration Path:</b> Saves event logs parsed from message consumers in a non-blocking relational schema.</p>
 * <p><b>Enterprise Patterns Used:</b> Data Access Object (DAO) Pattern / Repository Pattern.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Leverages Spring Data JPA to generate type-safe SQL queries automatically at runtime.
 * 2. Implements custom query boundaries to filter logs chronologically (date ranges), by user, by service origin, or by event type, matching index optimization targets.</p>
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Finds all audit logs associated with a specific user.
     *
     * @param userId the user's UUID
     * @return the list of matching audit logs, sorted by event timestamp descending
     */
    List<AuditLog> findByUserIdOrderByEventTimestampDesc(UUID userId);

    /**
     * Finds all audit logs matching a specific event classification.
     *
     * @param eventType the type of audit event
     * @return the list of matching audit logs, sorted by event timestamp descending
     */
    List<AuditLog> findByEventTypeOrderByEventTimestampDesc(AuditEventType eventType);

    /**
     * Finds all audit logs recorded in a designated date-time range.
     *
     * @param start start of chronological filter boundary
     * @param end end of chronological filter boundary
     * @return the list of matching audit logs, sorted by event timestamp descending
     */
    List<AuditLog> findByEventTimestampBetweenOrderByEventTimestampDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Finds all audit logs originated from a specific microservice.
     *
     * @param serviceName the name of the microservice
     * @return the list of matching audit logs, sorted by event timestamp descending
     */
    List<AuditLog> findByServiceNameOrderByEventTimestampDesc(String serviceName);
}
