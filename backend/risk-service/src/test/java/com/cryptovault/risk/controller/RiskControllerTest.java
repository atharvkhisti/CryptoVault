package com.cryptovault.risk.controller;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.RiskLevel;
import com.cryptovault.common.enums.RiskStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.RiskResponse;
import com.cryptovault.risk.service.RiskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RiskController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false) // Bypass spring security filters for controller logic isolation testing
class RiskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskService riskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser
    void evaluateRisk_ShouldReturn201Created_WhenPayloadIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .userId(userId)
                .transactionId(transactionId)
                .amount(new BigDecimal("500"))
                .currency(CurrencyType.INR)
                .type(TransactionType.TRANSFER)
                .build();

        RiskResponse mockResponse = RiskResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .transactionId(transactionId)
                .riskScore(0)
                .riskLevel(RiskLevel.LOW)
                .status(RiskStatus.APPROVED)
                .comments("Approved")
                .createdAt(LocalDateTime.now())
                .build();

        when(riskService.evaluateTransactionRisk(any(EvaluateRiskRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Risk evaluation completed successfully"))
                .andExpect(jsonPath("$.data.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(riskService, times(1)).evaluateTransactionRisk(any(EvaluateRiskRequest.class));
    }

    @Test
    @WithMockUser
    void getRiskAssessment_ShouldReturn200Ok_WhenAssessmentExists() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        RiskResponse mockResponse = RiskResponse.builder()
                .id(assessmentId)
                .userId(UUID.randomUUID())
                .riskScore(40)
                .riskLevel(RiskLevel.MEDIUM)
                .status(RiskStatus.APPROVED)
                .comments("Approved")
                .createdAt(LocalDateTime.now())
                .build();

        when(riskService.getRiskAssessment(assessmentId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/risk/{id}", assessmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.data.riskLevel").value("MEDIUM"));

        verify(riskService, times(1)).getRiskAssessment(assessmentId);
    }

    @Test
    @WithMockUser
    void getUserRiskHistory_ShouldReturn200Ok_WhenHistoryRequested() throws Exception {
        UUID userId = UUID.randomUUID();
        RiskResponse mockResponse = RiskResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .riskScore(70)
                .riskLevel(RiskLevel.HIGH)
                .status(RiskStatus.FLAGGED)
                .createdAt(LocalDateTime.now())
                .build();

        when(riskService.getUserRiskHistory(userId)).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/risk/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.data[0].riskLevel").value("HIGH"));

        verify(riskService, times(1)).getUserRiskHistory(userId);
    }

    @Test
    @WithMockUser
    void getAllAssessments_ShouldReturn200Ok_WhenAllRequested() throws Exception {
        when(riskService.getAllAssessments()).thenReturn(List.of());

        mockMvc.perform(get("/api/risk/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All risk assessments history retrieved successfully"));

        verify(riskService, times(1)).getAllAssessments();
    }
}
