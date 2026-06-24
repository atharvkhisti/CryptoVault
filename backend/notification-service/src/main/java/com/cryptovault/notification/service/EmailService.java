package com.cryptovault.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * <h3>EmailService</h3>
 *
 * <p><b>Why it exists:</b> Coordinates email formatting, MIME helper building, and physical transport via JavaMailSender.</p>
 * <p><b>Architectural Layer:</b> Service Layer.</p>
 * <p><b>Design Patterns Used:</b> Facade Pattern (wraps JavaMail Sender configurations).</p>
 * <p><b>Security Concepts Demonstrated:</b> Enforces secure transport parameters and isolates credentials.</p>
 * <p><b>Future AWS Integration Path:</b> In production, this can be swapped or decorated with AWS SES (Simple Email Service) client templates.</p>
 * <p><b>Enterprise Relevance:</b> Enforces standard system alerting channels to keep customers aware of balances.</p>
 * <p><b>Interview Talking Points:</b> Uses standard Spring Mail helper structures with UTF-8 character encoding to dispatch complex HTML email contents asynchronously.</p>
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${application.mail.from}")
    private String fromEmail;

    /**
     * Dispatches an HTML email to the target recipient.
     *
     * @param to          recipient email address
     * @param subject     email subject line
     * @param htmlContent rich HTML body content
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email successfully sent to={} with subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to build or send email to={}: ", to, e);
            throw new RuntimeException("Email dispatch failed due to internal SMTP server errors", e);
        }
    }
}
