package com.cryptovault.notification.controller;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.notification.dto.request.SendNotificationRequest;
import com.cryptovault.notification.dto.response.NotificationResponse;
import com.cryptovault.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <h3>NotificationController</h3>
 *
 * <p><b>Why it exists:</b> Exposes REST API endpoints for dispatching emails and querying historical alerts.</p>
 * <p><b>Architectural Layer:</b> Web / Controller Layer.</p>
 * <p><b>Design Patterns Used:</b> Controller / Presenter Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Validates input structures via <code>@Valid</code> and relies on Spring Security for endpoint protection.</p>
 * <p><b>Future AWS Integration Path:</b> Co-exists with SQS Message Consumers, permitting REST triggers alongside event queue polling.</p>
 * <p><b>Enterprise Relevance:</b> Enforces standard client-facing REST endpoints returning uniform HTTP status codes.</p>
 * <p><b>Interview Talking Points:</b> Serves as the web ingress layer. Converts service outcomes into generic response wraps ({@link ApiResponse}) and wraps them in explicit {@link ResponseEntity} blocks.</p>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Registry Interface", description = "Endpoints for scheduling, sending, and auditing in-app and email alert histories")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    /**
     * Triggers email notification dispatch.
     *
     * @param request target recipient, type, and variables payload
     * @return tracking details
     */
    @PostMapping("/send")
    @Operation(summary = "Send Notification", description = "Enqueues and dispatches an email or in-app notification alert to the specified user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Notification enqueued and sent successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        log.info("REST call to dispatch notification for user={}", request.getUserId());
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent successfully", response));
    }

    /**
     * Returns history logs for a specific user.
     *
     * @param userId user UUID
     * @return list of sent notifications
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Notification History", description = "Retrieves all historically sent alerts and notification dispatches for a target user UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User notifications retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User notifications list not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable("userId") @Parameter(description = "Unique UUID of the user context", example = "123e4567-e89b-12d3-a456-426614174000") UUID userId
    ) {
        log.info("REST call to retrieve notifications log for user={}", userId);
        List<NotificationResponse> response = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("User notifications retrieved successfully", response));
    }

    /**
     * Returns a specific notification details by its UUID.
     *
     * @param id notification UUID
     * @return notification details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Notification Details", description = "Retrieves specific dispatch details and delivery status of a logged notification record.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification record not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @PathVariable("id") @Parameter(description = "Unique UUID of the notification record", example = "a2bf8a59-122e-407b-a1bc-cd14c2b9a799") UUID id
    ) {
        log.info("REST call to retrieve notification details for ID={}", id);
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success("Notification retrieved successfully", response));
    }

    /**
     * Returns all notifications recorded.
     *
     * @return list of all notifications responses
     */
    @GetMapping
    @Operation(summary = "Get All Logged Notifications", description = "Retrieves all notification transmission records registered across the entire system (admin operation).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All platform notifications retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications() {
        log.info("REST call to retrieve all logged notifications");
        List<NotificationResponse> response = notificationService.getAllNotifications();
        return ResponseEntity.ok(ApiResponse.success("All platform notifications retrieved successfully", response));
    }
}
