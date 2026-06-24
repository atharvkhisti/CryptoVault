package com.cryptovault.notification.dto.request;

import com.cryptovault.common.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * <h3>SendNotificationRequest</h3>
 *
 * <p><b>Why it exists:</b> Data payload container representing incoming requests to enqueue and dispatch custom notifications.</p>
 * <p><b>Architectural Layer:</b> Data Transfer Object / Request Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Edge input validation (guards against email spoofing or null inputs).</p>
 * <p><b>Future AWS Integration Path:</b> Aligns with the JSON structure mapped from consumed SQS messages.</p>
 * <p><b>Enterprise Relevance:</b> Clean request boundaries verifying correct payload structure before executing email transmissions.</p>
 * <p><b>Interview Talking Points:</b> Integrates standard Spring validation annotations to filter inputs early at the controller boundary.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for dispatching a notification alert")
public class SendNotificationRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "UUID of the user recipient", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID userId;

    @Email(message = "Recipient email address must be structured correctly")
    @NotBlank(message = "Recipient email address is required")
    @Schema(description = "Destination email address for dispatching alerts", example = "user@cryptovault.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotNull(message = "Notification type is required")
    @Schema(description = "Triggering event classification type", example = "DEPOSIT_SUCCESSFUL", requiredMode = Schema.RequiredMode.REQUIRED)
    private NotificationType type;

    @NotBlank(message = "Subject line is required")
    @Schema(description = "Email subject title header line", example = "CryptoVault Deposit Confirmed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @NotBlank(message = "Notification message content is required")
    @Schema(description = "Rich text or plain text notification content body", example = "Your deposit of 0.05 BTC has been successfully credited to your wallet.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}
