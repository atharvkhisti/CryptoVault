package com.cryptovault.audit.dto.request;

import com.cryptovault.common.enums.AuditEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * <h3>AuditEventRequest</h3>
 *
 * <p><b>Why it exists:</b> Payload DTO received by the controller to log a new auditable platform event.</p>
 * <p><b>Architectural Layer:</b> DTO / Interface Layer.</p>
 * <p><b>Compliance Relevance:</b> Standardizes the inputs required to trigger an audit record entry, enforcing mandatory metadata checks (such as origin IP, service name, action details).</p>
 * <p><b>Event-Driven Integration Path:</b> Parsed from deserialized JSON payloads off microservices REST Feign calls or queue messages.</p>
 * <p><b>Enterprise Patterns Used:</b> Data Transfer Object (DTO) Pattern.</p>
 * <p><b>Interview Talking Points:</b> Uses standard bean validation constraints (<code>@NotNull</code>, <code>@NotBlank</code>) to intercept validation errors before persistence, returning clean, structured validation error details.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for registering a new audit security/compliance event")
public class AuditEventRequest {

    @Schema(description = "UUID of the user associated with this event", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @NotNull(message = "Event type must not be null")
    @Schema(description = "Classification category of the audit event", example = "USER_LOGIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private AuditEventType eventType;

    @NotBlank(message = "Service name must not be blank")
    @Schema(description = "Originating microservice name dispatching this log", example = "auth-service", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    @NotBlank(message = "Action description must not be blank")
    @Schema(description = "Functional action executed within the platform boundary", example = "USER_LOGIN_SUCCESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String action;

    @NotBlank(message = "Detailed description must not be blank")
    @Schema(description = "Verbose description of the auditable transaction event details", example = "User logged in from browser user-agent.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotBlank(message = "IP address must not be blank")
    @Schema(description = "Client request network IP address context", example = "192.168.1.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ipAddress;
}
