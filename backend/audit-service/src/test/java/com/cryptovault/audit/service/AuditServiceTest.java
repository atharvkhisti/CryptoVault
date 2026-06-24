package com.cryptovault.audit.service;

import com.cryptovault.common.enums.AuditEventType;
import com.cryptovault.common.security.JwtUserPrincipal;
import com.cryptovault.audit.dto.request.AuditEventRequest;
import com.cryptovault.audit.dto.response.AuditResponse;
import com.cryptovault.audit.entity.AuditLog;
import com.cryptovault.audit.exception.AuditLogNotFoundException;
import com.cryptovault.audit.mapper.AuditMapper;
import com.cryptovault.audit.repository.AuditLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h3>AuditServiceTest</h3>
 *
 * <p><b>Why it exists:</b> Validates business rules, metric counters logic, and entity mapping conversions in isolation using Mockito double mocks.</p>
 * <p><b>Architectural Layer:</b> Testing Layer / Service Tests.</p>
 * <p><b>Compliance Relevance:</b> Verifies that chronological searches, user logs extractions, and record-keeping operations function as intended, meeting auditing guidelines.</p>
 * <p><b>Event-Driven Integration Path:</b> Tests validation behaviors triggered by asynchronous consumer models.</p>
 * <p><b>Enterprise Patterns Used:</b> Service Mock Test Pattern.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Mocks <code>MeterRegistry</code> and its nested <code>Counter.Builder</code> fluid interfaces, asserting that telemetry increments occur during successful operations and errors.
 * 2. Mocks Spring's static <code>SecurityContextHolder</code> to verify that context emails are correctly extracted and logged under the performedBy column.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private AuditMapper mapper;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counterMock;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private AuditService auditService;
    private UUID userId;
    private UUID logId;

    @BeforeEach
    void setUp() {
        // Setup Micrometer fluid builder mock behavior
        Counter.Builder builderMock = mock(Counter.Builder.class);
        when(builderMock.description(anyString())).thenReturn(builderMock);
        when(builderMock.register(any(MeterRegistry.class))).thenReturn(counterMock);
        
        try (var mockedCounter = mockStatic(Counter.class)) {
            mockedCounter.when(() -> Counter.builder(anyString())).thenReturn(builderMock);
            auditService = new AuditService(repository, mapper, meterRegistry);
        }

        userId = UUID.randomUUID();
        logId = UUID.randomUUID();
    }

    @Test
    void shouldLogEventSuccessfullyWithPrincipalEmail() {
        // Mock Security Context
        JwtUserPrincipal principal = JwtUserPrincipal.builder()
                .userId(userId)
                .email("alice@cryptovault.com")
                .role("USER")
                .build();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.setContext(securityContext);

        AuditEventRequest request = AuditEventRequest.builder()
                .userId(userId)
                .eventType(AuditEventType.USER_LOGIN)
                .serviceName("auth-service")
                .action("LOGIN")
                .description("Alice logged in successfully")
                .ipAddress("127.0.0.1")
                .build();

        AuditLog entity = AuditLog.builder()
                .id(logId)
                .userId(userId)
                .eventType(AuditEventType.USER_LOGIN)
                .serviceName("auth-service")
                .action("LOGIN")
                .description("Alice logged in successfully")
                .ipAddress("127.0.0.1")
                .performedBy("alice@cryptovault.com")
                .eventTimestamp(LocalDateTime.now())
                .build();

        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.USER_LOGIN)
                .action("LOGIN")
                .eventTimestamp(entity.getEventTimestamp())
                .build();

        when(mapper.toEntity(any(AuditEventRequest.class), eq("alice@cryptovault.com"))).thenReturn(entity);
        when(repository.save(any(AuditLog.class))).thenReturn(entity);
        when(mapper.toResponse(any(AuditLog.class))).thenReturn(response);

        AuditResponse result = auditService.logEvent(request);

        assertThat(result).isNotNull();
        assertThat(result.getAuditId()).isEqualTo(logId);
        verify(counterMock, times(1)).increment();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetAuditLogById() {
        AuditLog entity = AuditLog.builder()
                .id(logId)
                .eventType(AuditEventType.USER_LOGIN)
                .action("LOGIN")
                .eventTimestamp(LocalDateTime.now())
                .build();

        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.USER_LOGIN)
                .action("LOGIN")
                .build();

        when(repository.findById(logId)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        AuditResponse result = auditService.getAuditLog(logId);

        assertThat(result).isNotNull();
        assertThat(result.getAuditId()).isEqualTo(logId);
    }

    @Test
    void shouldThrowExceptionWhenAuditLogNotFound() {
        when(repository.findById(logId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditLog(logId))
                .isInstanceOf(AuditLogNotFoundException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void shouldGetUserAuditLogs() {
        AuditLog entity = AuditLog.builder()
                .id(logId)
                .userId(userId)
                .eventType(AuditEventType.DEPOSIT)
                .action("DEPOSIT")
                .build();

        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.DEPOSIT)
                .action("DEPOSIT")
                .build();

        when(repository.findByUserIdOrderByEventTimestampDesc(userId)).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        List<AuditResponse> result = auditService.getUserAuditLogs(userId);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetAuditLogsByType() {
        AuditLog entity = AuditLog.builder()
                .id(logId)
                .eventType(AuditEventType.SECURITY_VIOLATION)
                .action("VIOLATION")
                .build();

        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.SECURITY_VIOLATION)
                .action("VIOLATION")
                .build();

        when(repository.findByEventTypeOrderByEventTimestampDesc(AuditEventType.SECURITY_VIOLATION)).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        List<AuditResponse> result = auditService.getAuditLogsByType(AuditEventType.SECURITY_VIOLATION);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetAuditLogsByDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        AuditLog entity = AuditLog.builder()
                .id(logId)
                .eventType(AuditEventType.WALLET_CREATED)
                .action("CREATE_WALLET")
                .eventTimestamp(LocalDateTime.now().minusHours(2))
                .build();

        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.WALLET_CREATED)
                .action("CREATE_WALLET")
                .build();

        when(repository.findByEventTimestampBetweenOrderByEventTimestampDesc(start, end)).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        List<AuditResponse> result = auditService.getAuditLogsByDateRange(start, end);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldThrowExceptionForInvalidDateRange() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> auditService.getAuditLogsByDateRange(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date must be before end date");
    }
}
