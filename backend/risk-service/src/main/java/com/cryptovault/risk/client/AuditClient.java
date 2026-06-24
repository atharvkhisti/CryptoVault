package com.cryptovault.risk.client;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.risk.config.FeignClientConfig;
import com.cryptovault.risk.dto.response.AuditResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * <h3>AuditClient</h3>
 *
 * <p><b>Why it exists:</b> Feign client defining REST integrations to pull audit logs history.</p>
 * <p><b>Architectural Layer:</b> Integration / Client Layer.</p>
 * <p><b>Design Patterns Used:</b> Proxy Pattern (Feign abstraction).</p>
 */
@FeignClient(
        name = "audit-service",
        url = "${application.client.audit-service.url:http://localhost:8086}",
        configuration = FeignClientConfig.class
)
public interface AuditClient {

    /**
     * Fetch user audit logs history records downstream.
     *
     * @param userId user identifier
     * @return standard response envelope wrapping list of audit logs
     */
    @GetMapping("/api/audit/user/{userId}")
    ApiResponse<List<AuditResponse>> getUserAuditLogs(@PathVariable("userId") UUID userId);
}
