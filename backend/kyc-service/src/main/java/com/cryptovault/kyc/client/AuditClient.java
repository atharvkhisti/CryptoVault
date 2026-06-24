package com.cryptovault.kyc.client;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.kyc.config.FeignClientConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.cryptovault.common.enums.AuditEventType;

import java.util.UUID;

/**
 * <h3>AuditClient</h3>
 *
 * <p><b>Why it exists:</b> Feign client defining REST integrations to dispatch compliance and security log messages to the Audit microservice.</p>
 * <p><b>Architectural Layer:</b> Integration / Feign Client Layer.</p>
 * <p><b>Design Patterns Used:</b> Proxy Pattern / Declarative REST Client.</p>
 * <p><b>Financial Compliance Relevance:</b> Safely logs KYC events (KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED) into the immutable audit database.</p>
 */
@FeignClient(
        name = "audit-service",
        url = "${application.client.audit-service.url:http://localhost:8086}",
        configuration = FeignClientConfig.class
)
public interface AuditClient {

    /**
     * Record a new audit log compliance entry.
     *
     * @param request event payload
     * @return empty api response wrapper
     */
    @PostMapping("/api/audit")
    ApiResponse<Void> logEvent(@RequestBody AuditEventRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AuditEventRequest {
        private UUID userId;
        private AuditEventType eventType;
        private String serviceName;
        private String action;
        private String description;
        private String ipAddress;
    }
}
