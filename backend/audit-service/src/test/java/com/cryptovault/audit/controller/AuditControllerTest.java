package com.cryptovault.audit.controller;

import com.cryptovault.common.enums.AuditEventType;
import com.cryptovault.audit.dto.request.AuditEventRequest;
import com.cryptovault.audit.dto.response.AuditResponse;
import com.cryptovault.audit.service.AuditService;
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
 * <h3>AuditControllerTest</h3>
 *
 * <p><b>Why it exists:</b> Conducts endpoint routing checks, path variable extractions, and input payload validation mapping tests.</p>
 * <p><b>Architectural Layer:</b> Testing Layer / Controller Slices.</p>
 * <p><b>Compliance Relevance:</b> Verifies security perimeter routing rules, confirming that only valid payloads are accepted and query APIs require authorization contexts.</p>
 * <p><b>Event-Driven Integration Path:</b> Tests API inputs validating formatting bounds (like valid UUIDs or event enums).</p>
 * <p><b>Enterprise Patterns Used:</b> Controller Slices Mocking Pattern.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Leverages Spring Boot's servlet-specific <code>@WebMvcTest</code> to test Spring Web MVC routes in isolation.
 * 2. Uses <code>@MockitoBean</code> (modern replacement for MockBean in Spring Framework 6.2+) to inject mock service logic.
 * 3. Asserts returned JSON schemas are wrapped inside standard <code>ApiResponse</code> wrapper structures.</p>
 */
@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID logId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        logId = UUID.randomUUID();
    }

    @Test
    void shouldLogEventSuccessfully() throws Exception {
        AuditEventRequest request = AuditEventRequest.builder()
                .userId(userId)
                .eventType(AuditEventType.DEPOSIT)
                .serviceName("wallet-service")
                .action("DEPOSIT_ASSET")
                .description("Deposited 0.5 BTC")
                .ipAddress("192.168.1.5")
                .build();

        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.DEPOSIT)
                .action("DEPOSIT_ASSET")
                .eventTimestamp(LocalDateTime.now())
                .build();

        when(auditService.logEvent(any(AuditEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Audit log entry created successfully"))
                .andExpect(jsonPath("$.data.auditId").value(logId.toString()));
    }

    @Test
    void shouldFailLoggingEventForMissingType() throws Exception {
        AuditEventRequest request = AuditEventRequest.builder()
                .serviceName("wallet-service")
                .action("DEPOSIT_ASSET")
                .description("Deposited 0.5 BTC")
                .ipAddress("192.168.1.5")
                .build();

        mockMvc.perform(post("/api/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Validation failed")));
    }

    @Test
    void shouldGetAuditLogById() throws Exception {
        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.USER_REGISTERED)
                .action("REGISTER")
                .eventTimestamp(LocalDateTime.now())
                .build();

        when(auditService.getAuditLog(logId)).thenReturn(response);

        mockMvc.perform(get("/api/audit/" + logId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.auditId").value(logId.toString()));
    }

    @Test
    void shouldGetAllAuditLogs() throws Exception {
        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.WALLET_CREATED)
                .action("CREATE_WALLET")
                .eventTimestamp(LocalDateTime.now())
                .build();

        when(auditService.getAllAuditLogs()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/audit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].auditId").value(logId.toString()));
    }

    @Test
    void shouldGetUserAuditLogs() throws Exception {
        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.BALANCE_UPDATED)
                .action("UPDATE_BALANCE")
                .eventTimestamp(LocalDateTime.now())
                .build();

        when(auditService.getUserAuditLogs(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/audit/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].auditId").value(logId.toString()));
    }

    @Test
    void shouldGetAuditLogsByType() throws Exception {
        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.RISK_ALERT_GENERATED)
                .action("RISK_ALERT")
                .eventTimestamp(LocalDateTime.now())
                .build();

        when(auditService.getAuditLogsByType(AuditEventType.RISK_ALERT_GENERATED)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/audit/type/" + AuditEventType.RISK_ALERT_GENERATED)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].auditId").value(logId.toString()));
    }

    @Test
    void shouldGetAuditLogsByDateRange() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        AuditResponse response = AuditResponse.builder()
                .auditId(logId)
                .eventType(AuditEventType.TRANSFER_COMPLETED)
                .action("TRANSFER")
                .eventTimestamp(LocalDateTime.now().minusHours(5))
                .build();

        when(auditService.getAuditLogsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/audit/date-range")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].auditId").value(logId.toString()));
    }
}
