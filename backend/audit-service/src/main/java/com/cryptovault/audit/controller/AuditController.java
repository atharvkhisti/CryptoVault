package com.cryptovault.audit.controller;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.AuditEventType;
import com.cryptovault.audit.dto.request.AuditEventRequest;
import com.cryptovault.audit.dto.response.AuditResponse;
import com.cryptovault.audit.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <h3>AuditController</h3>
 *
 * <p><b>Why it exists:</b> Exposes REST API endpoints allowing other services to record platform events and auditing teams to query compliance logs.</p>
 * <p><b>Architectural Layer:</b> Controller / REST Interface Layer.</p>
 * <p><b>Compliance Relevance:</b> Provides the entry gate to retrieve auditable platform records, validating queries and requests bounds to protect sensitive information access tracks.</p>
 * <p><b>Event-Driven Integration Path:</b> Acts as the REST ingest fallback for services unable to publish to asynchronous queues.</p>
 * <p><b>Enterprise Patterns Used:</b> Front Controller Pattern / RESTful API Pattern.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Wraps all return envelopes in a generic, unified {@link ApiResponse} structure.
 * 2. Uses Spring validations via <code>@Valid</code> on payloads.
 * 3. Enforces date-range limits using <code>@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)</code> parameters.</p>
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Query Interface", description = "Endpoints for registering and querying immutable security, transaction, and compliance activity logs")
public class AuditController {

    private final AuditService auditService;

    /**
     * Records a new audit log event.
     *
     * @param request event details payload
     * @return 201 Created containing AuditResponse payload
     */
    @PostMapping
    @Operation(summary = "Log Audit Event", description = "Registers a new security or compliance audit record in the platform database.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Audit log entry created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<AuditResponse>> logEvent(@Valid @RequestBody AuditEventRequest request) {
        log.info("Received POST /api/audit eventType={}", request.getEventType());
        AuditResponse response = auditService.logEvent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Audit log entry created successfully", response));
    }

    /**
     * Retrieves a specific audit log by its unique UUID.
     *
     * @param id audit log UUID
     * @return 200 OK containing AuditResponse payload
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Audit Log Details", description = "Retrieves specific properties and details of a single audit log by its unique UUID identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit log details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Audit log record not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<AuditResponse>> getAuditLog(
            @PathVariable("id") @Parameter(description = "Unique UUID of the audit log record", example = "c2bf8a59-122e-407b-a1bc-cd14c2b9a800") UUID id
    ) {
        log.info("Received GET /api/audit/{}", id);
        AuditResponse response = auditService.getAuditLog(id);
        return ResponseEntity.ok(ApiResponse.success("Audit log details retrieved successfully", response));
    }

    /**
     * Retrieves all audit logs registered across the platform.
     *
     * @return 200 OK containing list of logs
     */
    @GetMapping
    @Operation(summary = "Get All Audit Logs", description = "Retrieves all audit logs registered in the system database (compliance admin query).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All audit logs retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<AuditResponse>>> getAllAuditLogs() {
        log.info("Received GET /api/audit");
        List<AuditResponse> response = auditService.getAllAuditLogs();
        return ResponseEntity.ok(ApiResponse.success("All audit logs retrieved successfully", response));
    }

    /**
     * Retrieves all audit logs registered for a specific user.
     *
     * @param userId user UUID
     * @return 200 OK containing list of matching logs
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Audit Logs", description = "Retrieves audit trails filtering by a specific user UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User audit logs retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User audit logs not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<AuditResponse>>> getUserAuditLogs(
            @PathVariable("userId") @Parameter(description = "Unique UUID of the user context", example = "123e4567-e89b-12d3-a456-426614174000") UUID userId
    ) {
        log.info("Received GET /api/audit/user/{}", userId);
        List<AuditResponse> response = auditService.getUserAuditLogs(userId);
        return ResponseEntity.ok(ApiResponse.success("User audit logs retrieved successfully", response));
    }

    /**
     * Retrieves all audit logs matching a specific event classification type.
     *
     * @param eventType the event type enum key string
     * @return 200 OK containing list of matching logs
     */
    @GetMapping("/type/{eventType}")
    @Operation(summary = "Filter Audit Logs by Type", description = "Retrieves audit trails filtering by the event classification type category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs by type retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<AuditResponse>>> getAuditLogsByType(
            @PathVariable("eventType") @Parameter(description = "Audit event classification category enum", example = "USER_LOGIN") AuditEventType eventType
    ) {
        log.info("Received GET /api/audit/type/{}", eventType);
        List<AuditResponse> response = auditService.getAuditLogsByType(eventType);
        return ResponseEntity.ok(ApiResponse.success("Audit logs by type retrieved successfully", response));
    }

    /**
     * Retrieves all audit logs registered within a specified date time range boundaries.
     *
     * @param start start range limit in ISO date time format
     * @param end end range limit in ISO date time format
     * @return 200 OK containing list of matching logs
     */
    @GetMapping("/date-range")
    @Operation(summary = "Filter Audit Logs by Date Range", description = "Retrieves audit trails filtering within a specific time boundary range.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs within date range retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<AuditResponse>>> getAuditLogsByDateRange(
            @RequestParam("start") @Parameter(description = "Start datetime range boundary limit in ISO format", example = "2026-06-19T00:00:00") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @Parameter(description = "End datetime range boundary limit in ISO format", example = "2026-06-19T23:59:59") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        log.info("Received GET /api/audit/date-range?start={}&end={}", start, end);
        List<AuditResponse> response = auditService.getAuditLogsByDateRange(start, end);
        return ResponseEntity.ok(ApiResponse.success("Audit logs within date range retrieved successfully", response));
    }
}
