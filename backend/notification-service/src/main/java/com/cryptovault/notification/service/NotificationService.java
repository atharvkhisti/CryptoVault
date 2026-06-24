package com.cryptovault.notification.service;

import com.cryptovault.common.enums.NotificationStatus;
import com.cryptovault.common.enums.NotificationType;
import com.cryptovault.notification.dto.request.SendNotificationRequest;
import com.cryptovault.notification.dto.response.NotificationResponse;
import com.cryptovault.notification.entity.Notification;
import com.cryptovault.notification.exception.NotificationNotFoundException;
import com.cryptovault.notification.mapper.NotificationMapper;
import com.cryptovault.notification.repository.NotificationRepository;
import com.cryptovault.notification.util.SimpleTemplateEngine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <h3>NotificationService</h3>
 *
 * <p><b>Why it exists:</b> Coordinates email triggers, processes HTML layouts via substitutions, and logs alert histories in PostgreSQL.</p>
 * <p><b>Architectural Layer:</b> Service / Business Logic Layer.</p>
 * <p><b>Design Patterns Used:</b> Facade Pattern (orchestrates template rendering and SMTP execution), Strategy Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Preserves a complete communication audit log, mapping actions back to unique customer user ID references.</p>
 * <p><b>Future AWS Integration Path:</b> Acts as the implementation method invoked by asynchronous AWS SQS message consumers.</p>
 * <p><b>Enterprise Relevance:</b> Enforces transactional execution of alert dispatches, ensuring database state is consistently updated to reflect SENT or FAILED delivery.</p>
 * <p><b>Interview Talking Points:</b> Standardizes transactional boundaries using <code>@Transactional</code>. Saves a PENDING trace first, executes SMTP triggers, and updates state inside a try-catch to prevent losing dispatch failure metrics.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpleTemplateEngine templateEngine;
    private final NotificationMapper notificationMapper;

    /**
     * Creates a database trace in a PENDING state, resolves HTML templates,
     * sends emails, and persists the delivery outcome.
     *
     * @param request sending request payload
     * @return tracking response details
     */
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Processing notification request for user={} type={}", request.getUserId(), request.getType());

        // 1. Log a historical audit trace in PENDING state
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .recipientEmail(request.getEmail())
                .type(request.getType())
                .status(NotificationStatus.PENDING)
                .subject(request.getSubject())
                .message(request.getMessage())
                .build();

        notification = notificationRepository.save(notification);

        try {
            // 2. Map and parse template contents
            String htmlContent;
            String templateName = getTemplateNameForType(request.getType());

            if (templateName != null) {
                Map<String, Object> vars = parseMessageVariables(request.getMessage());
                htmlContent = templateEngine.process(templateName, vars);
            } else {
                htmlContent = "<p>" + request.getMessage() + "</p>";
            }

            // 3. Dispatch MIME message
            emailService.sendHtmlEmail(request.getEmail(), request.getSubject(), htmlContent);

            // 4. Update state to SENT on success
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Email alert successfully sent to target address. Notification ID={}", notification.getId());
        } catch (Exception e) {
            log.error("Failed to execute notification send for ID={}: ", notification.getId(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
            throw new RuntimeException("Notification transmission failed: " + e.getMessage(), e);
        }

        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    /**
     * Specific helper to trigger transfer success alerts.
     */
    @Transactional
    public NotificationResponse sendTransferNotification(UUID userId, String email, String amount, String currency, String referenceNumber) {
        String variables = String.format("amount=%s,currency=%s,referenceNumber=%s", amount, currency, referenceNumber);
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .email(email)
                .type(NotificationType.TRANSFER)
                .subject("Fund Transfer Confirmed")
                .message(variables)
                .build();
        return sendNotification(request);
    }

    /**
     * Specific helper to trigger deposit success alerts.
     */
    @Transactional
    public NotificationResponse sendDepositNotification(UUID userId, String email, String amount, String currency, String referenceNumber) {
        String variables = String.format("amount=%s,currency=%s,referenceNumber=%s", amount, currency, referenceNumber);
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .email(email)
                .type(NotificationType.DEPOSIT)
                .subject("Deposit Confirmed")
                .message(variables)
                .build();
        return sendNotification(request);
    }

    /**
     * Specific helper to trigger withdrawal success alerts.
     */
    @Transactional
    public NotificationResponse sendWithdrawalNotification(UUID userId, String email, String amount, String currency, String referenceNumber) {
        String variables = String.format("amount=%s,currency=%s,referenceNumber=%s", amount, currency, referenceNumber);
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .email(email)
                .type(NotificationType.WITHDRAWAL)
                .subject("Withdrawal Confirmed")
                .message(variables)
                .build();
        return sendNotification(request);
    }

    /**
     * Retrieves all notifications sent to a user.
     *
     * @param userId user UUID
     * @return list of notification responses
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        log.info("Fetching notifications log history for user={}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves a single notification record by UUID.
     *
     * @param id notification UUID
     * @return details of notification response
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id) {
        log.info("Fetching notification log record for ID={}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification record not found with id: " + id));
        return notificationMapper.toResponse(notification);
    }

    /**
     * Retrieves all notifications logged on the platform.
     *
     * @return list of all notifications responses
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        log.info("Fetching all platform notifications history logs");
        return notificationRepository.findAll().stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    private String getTemplateNameForType(NotificationType type) {
        switch (type) {
            case REGISTRATION:
                return "registration.html";
            case DEPOSIT:
                return "deposit.html";
            case WITHDRAWAL:
                return "withdrawal.html";
            case TRANSFER:
                return "transfer.html";
            default:
                return null;
        }
    }

    private Map<String, Object> parseMessageVariables(String message) {
        Map<String, Object> vars = new HashMap<>();
        // Seed default mappings to prevent template compile issues if variables are missing
        vars.put("name", "User");
        vars.put("amount", "");
        vars.put("currency", "");
        vars.put("referenceNumber", "");
        vars.put("message", message);

        if (message == null || !message.contains("=")) {
            vars.put("name", message != null ? message : "User");
            return vars;
        }

        try {
            String[] pairs = message.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    vars.put(kv[0].trim(), kv[1].trim());
                }
            }
        } catch (Exception e) {
            log.warn("Variables parsing failed for: {}, using default map", message);
        }
        return vars;
    }
}
