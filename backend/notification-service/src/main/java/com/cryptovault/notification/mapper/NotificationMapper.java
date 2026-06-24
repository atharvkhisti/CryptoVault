package com.cryptovault.notification.mapper;

import com.cryptovault.notification.dto.response.NotificationResponse;
import com.cryptovault.notification.entity.Notification;
import org.springframework.stereotype.Component;

/**
 * <h3>NotificationMapper</h3>
 *
 * <p><b>Why it exists:</b> Translates internal Hibernate database entities into client-facing data transfer objects.</p>
 * <p><b>Architectural Layer:</b> Mapping / Utility Layer.</p>
 * <p><b>Design Patterns Used:</b> Data Mapper / Translator Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Protects internal object representation layers from leaking to public networks.</p>
 * <p><b>Future AWS Integration Path:</b> Converts persisted entity mappings back to standardized response payloads after SQS listeners trigger mail dispatch.</p>
 * <p><b>Enterprise Relevance:</b> Clean boundary mapping avoiding tight coupling between database schemas and client API contracts.</p>
 * <p><b>Interview Talking Points:</b> Direct programmatic mapping provides low compilation overhead and high performance compared to runtime reflection libraries.</p>
 */
@Component
public class NotificationMapper {

    /**
     * Converts a Notification entity to its NotificationResponse DTO representation.
     *
     * @param notification the persistence entity
     * @return the mapping DTO response
     */
    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .build();
    }
}
