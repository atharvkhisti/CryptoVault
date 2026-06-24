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
import com.cryptovault.common.enums.NotificationType;

import java.util.UUID;

/**
 * <h3>NotificationClient</h3>
 *
 * <p><b>Why it exists:</b> Feign client defining REST integrations to dispatch alerts to the Notification microservice.</p>
 * <p><b>Architectural Layer:</b> Integration / Feign Client Layer.</p>
 * <p><b>Design Patterns Used:</b> Proxy Pattern / Declarative REST Client.</p>
 * <p><b>Financial Compliance Relevance:</b> Dispatches vital compliance alert notifications (such as KYC Approvals or Rejections) to customers.</p>
 */
@FeignClient(
        name = "notification-service",
        url = "${application.client.notification-service.url:http://localhost:8084}",
        configuration = FeignClientConfig.class
)
public interface NotificationClient {

    /**
     * Dispatch email notification request.
     *
     * @param request target notification request
     * @return empty response wrapper
     */
    @PostMapping("/api/notifications/send")
    ApiResponse<Void> sendNotification(@RequestBody SendNotificationRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SendNotificationRequest {
        private UUID userId;
        private String email;
        private NotificationType type;
        private String subject;
        private String message;
    }
}
