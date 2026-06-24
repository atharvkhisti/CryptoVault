package com.cryptovault.audit.service;

import com.cryptovault.common.enums.AuditEventType;
import com.cryptovault.common.security.JwtUserPrincipal;
import com.cryptovault.audit.dto.request.AuditEventRequest;
import com.cryptovault.audit.dto.response.AuditResponse;
import com.cryptovault.audit.entity.AuditLog;
import com.cryptovault.audit.exception.AuditLogNotFoundException;
import com.cryptovault.audit.mapper.AuditMapper;
import com.cryptovault.audit.repository.AuditLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <h3>AuditService</h3>
 *
 * <p><b>Why it exists:</b> Coordinates audit creation flow, log history filter searches, and records custom observability telemetry metrics.</p>
 * <p><b>Architectural Layer:</b> Business Logic / Service Layer.</p>
 * <p><b>Compliance Relevance:</b> Implements core business logic for compliance tracking. Enforces record immutability rules and logs queries for access transparency.</p>
 * <p><b>Event-Driven Integration Path:</b> Coordinates event logging triggers from REST controllers and AWS SQS message queues.</p>
 * <p><b>Enterprise Patterns Used:</b> Service Facade Pattern / Transaction Script Pattern.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Leverages <code>MeterRegistry</code> to register and increment custom Prometheus indicators (<code>audit_events_created</code>, <code>audit_queries_executed</code>, <code>failed_audit_requests</code>).
 * 2. Employs <code>@Transactional</code> boundaries to guarantee database persist operations are completed atomically.
 * 3. Dynamically resolves <code>performedBy</code> context from spring security context principal emails or requests metadata fallback targets.</p>
 */
@Service
@Slf4j
public class AuditService {

    private final AuditLogRepository repository;
    private final AuditMapper mapper;
    private final MeterRegistry meterRegistry;

    private final Counter auditEventsCreatedCounter;
    private final Counter auditQueriesExecutedCounter;
    private final Counter failedAuditRequestsCounter;

    /**
     * DI constructor injecting repos, mapper, and metrics registries.
     *
     * @param repository persistence repository
     * @param mapper entity mapper
     * @param meterRegistry micrometer prometheus metrics registry
     */
    public AuditService(AuditLogRepository repository, AuditMapper mapper, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;

        // Configure Prometheus metrics counters
        this.auditEventsCreatedCounter = Counter.builder("audit_events_created")
                .description("Total number of successfully logged audit events")
                .register(meterRegistry);

        this.auditQueriesExecutedCounter = Counter.builder("audit_queries_executed")
                .description("Total number of execution queries performed on audit logs")
                .register(meterRegistry);

        this.failedAuditRequestsCounter = Counter.builder("failed_audit_requests")
                .description("Total number of failed audit requests")
                .register(meterRegistry);
    }

    /**
     * Persists a new audit log record.
     *
     * @param request the audit details to record
     * @return the Response confirmation mapping containing ID and timestamp
     */
    @Transactional
    public AuditResponse logEvent(AuditEventRequest request) {
        try {
            log.info("Processing audit log request for eventType={} service={}", request.getEventType(), request.getServiceName());

            // Resolve performedBy user metadata from current security context
            String performedBy = "SYSTEM";
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
                performedBy = principal.getEmail();
            } else if (request.getUserId() != null) {
                performedBy = request.getUserId().toString();
            } else {
                performedBy = request.getServiceName();
            }

            AuditLog logEntity = mapper.toEntity(request, performedBy);
            AuditLog savedLog = repository.save(logEntity);

            // Increment metric
            auditEventsCreatedCounter.increment();

            return mapper.toResponse(savedLog);
        } catch (Exception e) {
            failedAuditRequestsCounter.increment();
            log.error("Failed to log audit event", e);
            throw e;
        }
    }

    /**
     * Retrieves a specific audit log by its unique UUID.
     *
     * @param id target audit log ID
     * @return response detail mapping
     * @throws AuditLogNotFoundException if ID doesn't exist
     */
    @Transactional(readOnly = true)
    public AuditResponse getAuditLog(UUID id) {
        auditQueriesExecutedCounter.increment();
        AuditLog logEntity = repository.findById(id)
                .orElseThrow(() -> {
                    failedAuditRequestsCounter.increment();
                    return new AuditLogNotFoundException(id);
                });
        return mapper.toResponse(logEntity);
    }

    /**
     * Retrieves all audit logs registered across the platform.
     *
     * @return list of response mappings
     */
    @Transactional(readOnly = true)
    public List<AuditResponse> getAllAuditLogs() {
        auditQueriesExecutedCounter.increment();
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all logs mapping to a specific user context.
     *
     * @param userId the user's UUID
     * @return list of matching response mappings
     */
    @Transactional(readOnly = true)
    public List<AuditResponse> getUserAuditLogs(UUID userId) {
        auditQueriesExecutedCounter.increment();
        return repository.findByUserIdOrderByEventTimestampDesc(userId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all logs mapping to a specific event type.
     *
     * @param eventType the target event type enum
     * @return list of matching response mappings
     */
    @Transactional(readOnly = true)
    public List<AuditResponse> getAuditLogsByType(AuditEventType eventType) {
        auditQueriesExecutedCounter.increment();
        return repository.findByEventTypeOrderByEventTimestampDesc(eventType).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all logs recorded within a specified date time range.
     *
     * @param start start range limit
     * @param end end range limit
     * @return list of matching response mappings
     */
    @Transactional(readOnly = true)
    public List<AuditResponse> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || start.isAfter(end)) {
            failedAuditRequestsCounter.increment();
            throw new IllegalArgumentException("Start date must be before end date and neither can be null");
        }
        auditQueriesExecutedCounter.increment();
        return repository.findByEventTimestampBetweenOrderByEventTimestampDesc(start, end).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all logs originated from a specific service.
     *
     * @param serviceName microservice classification name
     * @return list of matching response mappings
     */
    @Transactional(readOnly = true)
    public List<AuditResponse> getAuditLogsByService(String serviceName) {
        auditQueriesExecutedCounter.increment();
        return repository.findByServiceNameOrderByEventTimestampDesc(serviceName).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}
