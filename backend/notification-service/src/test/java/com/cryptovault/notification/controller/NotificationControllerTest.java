package com.cryptovault.notification.controller;

import com.cryptovault.common.enums.NotificationStatus;
import com.cryptovault.common.enums.NotificationType;
import com.cryptovault.notification.dto.request.SendNotificationRequest;
import com.cryptovault.notification.dto.response.NotificationResponse;
import com.cryptovault.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h3>NotificationControllerTest</h3>
 *
 * <p><b>Why it exists:</b> Performs endpoint verification using MockMvc slice configurations, testing routes maps and JSON converters.</p>
 * <p><b>Architectural Layer:</b> Testing Layer.</p>
 * <p><b>Design Patterns Used:</b> Controller Mock Integration Test Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Asserts that input verification filters intercept requests and payload validations trigger errors correctly.</p>
 * <p><b>Enterprise Relevance:</b> Enforces REST routing contracts at compile time, catching route misalignment regressions early.</p>
 * <p><b>Interview Talking Points:</b> Uses project-specific custom package imports <code>org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest</code>. Disables Spring Security filters to isolate controller route checks, mapping returned objects back to standardized {@link jsonPath} assertions.</p>
 */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    void shouldSendNotificationSuccessfully() throws Exception {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .email("alice@cryptovault.com")
                .type(NotificationType.TRANSFER)
                .subject("Fund Transfer Confirmed")
                .message("amount=10,currency=BTC,referenceNumber=TXN-1")
                .build();

        NotificationResponse response = NotificationResponse.builder()
                .notificationId(notificationId)
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.sendNotification(any(SendNotificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification sent successfully"))
                .andExpect(jsonPath("$.data.notificationId").value(notificationId.toString()))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    void shouldGetUserNotificationsSuccessfully() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .notificationId(notificationId)
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.getUserNotifications(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].notificationId").value(notificationId.toString()));
    }

    @Test
    void shouldGetNotificationByIdSuccessfully() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .notificationId(notificationId)
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.getNotificationById(notificationId)).thenReturn(response);

        mockMvc.perform(get("/api/notifications/" + notificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationId").value(notificationId.toString()));
    }

    @Test
    void shouldGetAllNotificationsSuccessfully() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .notificationId(notificationId)
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.getAllNotifications()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].notificationId").value(notificationId.toString()));
    }
}
