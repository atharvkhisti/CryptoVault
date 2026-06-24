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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h3>NotificationServiceTest</h3>
 *
 * <p><b>Why it exists:</b> Validates business rules, template transformations, state audit transitions, and lookup constraints within {@link NotificationService}.</p>
 * <p><b>Architectural Layer:</b> Testing Layer.</p>
 * <p><b>Design Patterns Used:</b> Unit Test Mock / Spy Patterns.</p>
 * <p><b>Security Concepts Demonstrated:</b> Asserts that state values (like PENDING/SENT/FAILED) and ownership boundaries are maintained without bypass gaps.</p>
 * <p><b>Enterprise Relevance:</b> Secures business transaction updates; ensures SMTP server timeouts automatically flag DB records as FAILED without locking databases.</p>
 * <p><b>Interview Talking Points:</b> Uses standard mock assertions. Tests that individual transfer, deposit, or withdrawal helper triggers correctly compile data maps and delegate mime dispatches.</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SimpleTemplateEngine templateEngine;

    @Spy
    private NotificationMapper notificationMapper = new NotificationMapper();

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private String email;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "alice@cryptovault.com";
    }

    @Test
    void shouldSendNotificationSuccessfully() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .email(email)
                .type(NotificationType.TRANSFER)
                .subject("Fund Transfer Confirmed")
                .message("amount=10,currency=BTC,referenceNumber=TXN-1")
                .build();

        Notification pendingNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientEmail(email)
                .type(NotificationType.TRANSFER)
                .status(NotificationStatus.PENDING)
                .subject(request.getSubject())
                .message(request.getMessage())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(pendingNotification);
        when(templateEngine.process(anyString(), anyMap())).thenReturn("<html>HTML Content</html>");

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals(NotificationStatus.SENT, pendingNotification.getStatus());
        verify(emailService, times(1)).sendHtmlEmail(eq(email), eq(request.getSubject()), anyString());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void shouldMarkAsFailedWhenEmailDispatchFails() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .email(email)
                .type(NotificationType.TRANSFER)
                .subject("Fund Transfer Confirmed")
                .message("amount=10,currency=BTC,referenceNumber=TXN-1")
                .build();

        Notification pendingNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientEmail(email)
                .type(NotificationType.TRANSFER)
                .status(NotificationStatus.PENDING)
                .subject(request.getSubject())
                .message(request.getMessage())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(pendingNotification);
        when(templateEngine.process(anyString(), anyMap())).thenReturn("<html>HTML Content</html>");
        doThrow(new RuntimeException("SMTP transport layer failure")).when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> notificationService.sendNotification(request));
        assertEquals(NotificationStatus.FAILED, pendingNotification.getStatus());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void shouldSendTransferNotificationSuccessfully() {
        Notification pendingNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientEmail(email)
                .type(NotificationType.TRANSFER)
                .status(NotificationStatus.PENDING)
                .subject("Fund Transfer Confirmed")
                .message("amount=1.5,currency=ETH,referenceNumber=TXN-2")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(pendingNotification);
        when(templateEngine.process(anyString(), anyMap())).thenReturn("<html>HTML Content</html>");

        NotificationResponse response = notificationService.sendTransferNotification(userId, email, "1.5", "ETH", "TXN-2");

        assertNotNull(response);
        verify(emailService, times(1)).sendHtmlEmail(eq(email), eq("Fund Transfer Confirmed"), anyString());
    }

    @Test
    void shouldGetUserNotificationsSuccessfully() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientEmail(email)
                .type(NotificationType.TRANSFER)
                .status(NotificationStatus.SENT)
                .subject("Fund Transfer Confirmed")
                .message("amount=1.5,currency=ETH,referenceNumber=TXN-2")
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notification));

        List<NotificationResponse> list = notificationService.getUserNotifications(userId);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(notification.getId(), list.get(0).getNotificationId());
    }

    @Test
    void shouldGetNotificationByIdSuccessfully() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(id)
                .userId(userId)
                .recipientEmail(email)
                .type(NotificationType.TRANSFER)
                .status(NotificationStatus.SENT)
                .subject("Fund Transfer Confirmed")
                .message("amount=1.5,currency=ETH,referenceNumber=TXN-2")
                .build();

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotificationById(id);

        assertNotNull(response);
        assertEquals(id, response.getNotificationId());
    }

    @Test
    void shouldThrowExceptionWhenNotificationByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getNotificationById(id));
    }

    @Test
    void shouldGetAllNotificationsSuccessfully() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientEmail(email)
                .type(NotificationType.TRANSFER)
                .status(NotificationStatus.SENT)
                .subject("Fund Transfer Confirmed")
                .message("amount=1.5,currency=ETH,referenceNumber=TXN-2")
                .build();

        when(notificationRepository.findAll()).thenReturn(List.of(notification));

        List<NotificationResponse> list = notificationService.getAllNotifications();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(notification.getId(), list.get(0).getNotificationId());
    }
}
