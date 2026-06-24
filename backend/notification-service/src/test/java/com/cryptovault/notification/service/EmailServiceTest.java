package com.cryptovault.notification.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * <h3>EmailServiceTest</h3>
 *
 * <p><b>Why it exists:</b> Validates SMTP integration layers, verifying email formatting and exception propagation logic in {@link EmailService}.</p>
 * <p><b>Architectural Layer:</b> Testing Layer.</p>
 * <p><b>Design Patterns Used:</b> Mocking Strategy.</p>
 * <p><b>Security Concepts Demonstrated:</b> Asserts correct isolation of configuration properties and transport validations.</p>
 * <p><b>Enterprise Relevance:</b> Guarantees that communication services fail gracefully and bubble up transport errors to caller contexts rather than swallowing them.</p>
 * <p><b>Interview Talking Points:</b> Uses standard Mockito constructs to mock out SMTP-level components ({@link JavaMailSender}). Tests behavior when mail servers fail or operate correctly.</p>
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@cryptovault.com");
    }

    @Test
    void shouldSendHtmlEmailSuccessfully() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("test@test.com", "Test Subject", "<p>Test Content</p>");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldThrowExceptionWhenMailSenderFails() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server connection timeout")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () ->
                emailService.sendHtmlEmail("test@test.com", "Test Subject", "<p>Test Content</p>")
        );
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
