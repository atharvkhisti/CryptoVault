package com.cryptovault.notification.client;

import com.cryptovault.notification.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <h3>SqsPlaceholderListener</h3>
 *
 * <p><b>Why it exists:</b> Serves as an architectural template detailing the future asynchronous event-driven SQS integration path.</p>
 * <p><b>Architectural Layer:</b> Client / Message Listener Ingress Layer.</p>
 * <p><b>Design Patterns Used:</b> Observer Pattern / Message Endpoint Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Decouples service dependencies so that notification processing does not require sharing direct transaction authorization secrets.</p>
 * <p><b>Future AWS Integration Path:</b> Once the AWS Spring Cloud SQS dependency (<code>io.awspring.cloud:spring-cloud-aws-starter-sqs</code>) is added to the POM, developers can uncomment the SQS listener configurations to poll active queues.</p>
 * <p><b>Enterprise Relevance:</b> Removes tight coupling between Transaction and Notification services. Events are queued and processed asynchronously, reducing transaction commit times and ensuring notifications are retried independently on failures.</p>
 * <p><b>Interview Talking Points:</b> Designing with message queues improves service resilience. If the email service fails temporarily, messages are simply returned to the queue or sent to a Dead Letter Queue (DLQ) without crashing the wallet balance updates.</p>
 */
@Component
@RequiredArgsConstructor
public class SqsPlaceholderListener {

    private static final Logger log = LoggerFactory.getLogger(SqsPlaceholderListener.class);
    private final NotificationService notificationService;

    /**
     * Blue-print placeholder for consuming Transaction completed events.
     *
     * @param event the parsed transaction event DTO
     */
    // @SqsListener("TransactionCompletedQueue")
    public void consumeTransactionCompletedEvent(TransactionCompletedEvent event) {
        log.info("SQS Event Intercepted | TransactionCompletedEvent for txnId={}", event.getTransactionId());
        try {
            if ("TRANSFER".equalsIgnoreCase(event.getType())) {
                notificationService.sendTransferNotification(
                        event.getUserId(),
                        event.getEmail(),
                        event.getAmount().toString(),
                        event.getCurrency(),
                        event.getReferenceNumber()
                );
            } else if ("DEPOSIT".equalsIgnoreCase(event.getType())) {
                notificationService.sendDepositNotification(
                        event.getUserId(),
                        event.getEmail(),
                        event.getAmount().toString(),
                        event.getCurrency(),
                        event.getReferenceNumber()
                );
            } else if ("WITHDRAWAL".equalsIgnoreCase(event.getType())) {
                notificationService.sendWithdrawalNotification(
                        event.getUserId(),
                        event.getEmail(),
                        event.getAmount().toString(),
                        event.getCurrency(),
                        event.getReferenceNumber()
                );
            }
        } catch (Exception e) {
            log.error("Failed to process transaction event for SQS txnId={}: ", event.getTransactionId(), e);
            throw e; // Propagating throws ensures SQS keeps messages or shifts them to DLQs
        }
    }

    /**
     * Blue-print placeholder for consuming Wallet creation events.
     *
     * @param event the parsed wallet event DTO
     */
    // @SqsListener("WalletCreatedQueue")
    public void consumeWalletCreatedEvent(WalletCreatedEvent event) {
        log.info("SQS Event Intercepted | WalletCreatedEvent for walletId={}", event.getWalletId());
        try {
            String message = String.format("name=%s,currency=%s", event.getName(), event.getCurrency());
            notificationService.sendNotification(
                    com.cryptovault.notification.dto.request.SendNotificationRequest.builder()
                            .userId(event.getUserId())
                            .email(event.getEmail())
                            .type(com.cryptovault.common.enums.NotificationType.WALLET_CREATED)
                            .subject("New Wallet Created")
                            .message(message)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to process wallet event for SQS walletId={}: ", event.getWalletId(), e);
            throw e;
        }
    }

    /**
     * Blue-print placeholder for consuming Risk Alerts.
     *
     * @param event risk alert event DTO
     */
    // @SqsListener("RiskAlertQueue")
    public void consumeRiskAlertEvent(RiskAlertEvent event) {
        log.info("SQS Event Intercepted | RiskAlertEvent for userId={} severity={}", event.getUserId(), event.getSeverity());
        try {
            String message = String.format("Risk alert detected. Reason: %s. Action required.", event.getReason());
            notificationService.sendNotification(
                    com.cryptovault.notification.dto.request.SendNotificationRequest.builder()
                            .userId(event.getUserId())
                            .email(event.getEmail())
                            .type(com.cryptovault.common.enums.NotificationType.RISK_ALERT)
                            .subject("Security Alert: Risk Threshold Exceeded")
                            .message(message)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to process risk alert event for SQS userId={}: ", event.getUserId(), e);
            throw e;
        }
    }

    @Data
    public static class TransactionCompletedEvent {
        private UUID transactionId;
        private UUID userId;
        private String email;
        private String type;
        private BigDecimal amount;
        private String currency;
        private String referenceNumber;
    }

    @Data
    public static class WalletCreatedEvent {
        private UUID walletId;
        private UUID userId;
        private String email;
        private String name;
        private String currency;
    }

    @Data
    public static class RiskAlertEvent {
        private UUID userId;
        private String email;
        private String severity;
        private String reason;
    }
}
