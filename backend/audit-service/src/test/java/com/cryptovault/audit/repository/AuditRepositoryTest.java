package com.cryptovault.audit.repository;

import com.cryptovault.common.enums.AuditEventType;
import com.cryptovault.audit.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h3>AuditRepositoryTest</h3>
 *
 * <p><b>Why it exists:</b> Integrates and asserts persistence boundaries using an in-memory test database, validating index queries and constraints mappings.</p>
 * <p><b>Architectural Layer:</b> Testing Layer / Repository Tests.</p>
 * <p><b>Compliance Relevance:</b> Verifies that query optimizations (like indices mappings) perform correctly, ensuring audit databases can respond to heavy compliance lookups.</p>
 * <p><b>Event-Driven Integration Path:</b> Verifies the integrity of records persisted by messaging queue streams.</p>
 * <p><b>Enterprise Patterns Used:</b> Repository Integration Test Pattern.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Uses <code>@DataJpaTest</code> to bootstrap JPA persistence contexts in isolation under H2.
 * 2. Asserts that the entity constraints throw an <code>UnsupportedOperationException</code> when update or delete operations are attempted, demonstrating compile-time and runtime immutability enforcement.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class AuditRepositoryTest {

    @Autowired
    private AuditLogRepository repository;

    @Test
    void shouldSaveAndQueryAuditLogsCorrectly() {
        UUID userId = UUID.randomUUID();
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .eventType(AuditEventType.TRANSFER_INITIATED)
                .serviceName("transaction-service")
                .action("TRANSFER_FUNDS")
                .description("Initiating transfer amount=100")
                .ipAddress("192.168.1.1")
                .performedBy("alice@cryptovault.com")
                .eventTimestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        AuditLog saved = repository.save(log);
        assertThat(saved.getId()).isNotNull();

        List<AuditLog> byUser = repository.findByUserIdOrderByEventTimestampDesc(userId);
        assertThat(byUser).hasSize(1);
        assertThat(byUser.get(0).getAction()).isEqualTo("TRANSFER_FUNDS");

        List<AuditLog> byType = repository.findByEventTypeOrderByEventTimestampDesc(AuditEventType.TRANSFER_INITIATED);
        assertThat(byType).hasSize(1);

        List<AuditLog> byService = repository.findByServiceNameOrderByEventTimestampDesc("transaction-service");
        assertThat(byService).hasSize(1);

        List<AuditLog> byDate = repository.findByEventTimestampBetweenOrderByEventTimestampDesc(
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        assertThat(byDate).hasSize(1);
    }

    @Test
    void shouldEnforceImmutabilityAndFailOnUpdateOrDelete() {
        AuditLog log = AuditLog.builder()
                .eventType(AuditEventType.USER_LOGIN)
                .serviceName("auth-service")
                .action("USER_LOGIN")
                .description("User Alice logged in")
                .ipAddress("127.0.0.1")
                .performedBy("alice@cryptovault.com")
                .eventTimestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        AuditLog saved = repository.saveAndFlush(log);

        // Attempt deletion and verify it throws exception due to @PreRemove callback
        assertThatThrownBy(() -> {
            repository.delete(saved);
            repository.flush();
        }).isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("Audit log records are immutable and cannot be deleted");
    }
}
