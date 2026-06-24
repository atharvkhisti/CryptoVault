package com.cryptovault.risk.service;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.RiskLevel;
import com.cryptovault.common.enums.RiskStatus;
import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.risk.client.AuditClient;
import com.cryptovault.risk.client.TransactionClient;
import com.cryptovault.risk.client.WalletClient;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.RiskResponse;
import com.cryptovault.risk.dto.response.TransactionResponse;
import com.cryptovault.risk.entity.RiskAssessment;
import com.cryptovault.risk.mapper.RiskMapper;
import com.cryptovault.risk.repository.RiskAssessmentRepository;
import com.cryptovault.risk.rule.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private RiskAssessmentRepository repository;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private AuditClient auditClient;

    @Mock
    private WalletClient walletClient;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private RiskMapper mapper;
    private RiskService riskService;

    @BeforeEach
    void setUp() {
        mapper = new RiskMapper();

        // Instantiate concrete rules to test real rule evaluations
        List<RiskRule> concreteRules = List.of(
                new HighAmountRule(),
                new FrequentTransferRule(),
                new FailedTransferRule(),
                new RapidWithdrawalRule()
        );

        // Setup mock telemetry registry calls
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        riskService = new RiskService(
                repository,
                mapper,
                transactionClient,
                auditClient,
                walletClient,
                concreteRules,
                meterRegistry
        );
    }

    @Test
    void evaluateTransactionRisk_ShouldEvaluateLowRisk_WhenNoRulesTriggered() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .userId(userId)
                .transactionId(transactionId)
                .amount(new BigDecimal("100"))
                .currency(CurrencyType.INR)
                .type(TransactionType.TRANSFER)
                .build();

        // Stub Feign clients to return empty lists
        when(transactionClient.getTransactionHistory()).thenReturn(ApiResponse.success("success", List.of()));
        when(auditClient.getUserAuditLogs(userId)).thenReturn(ApiResponse.success("success", List.of()));

        // Stub repository save
        when(repository.save(any(RiskAssessment.class))).thenAnswer(invocation -> {
            RiskAssessment assessment = invocation.getArgument(0);
            assessment.setId(UUID.randomUUID());
            assessment.setCreatedAt(LocalDateTime.now());
            return assessment;
        });

        RiskResponse response = riskService.evaluateTransactionRisk(request);

        assertThat(response).isNotNull();
        assertThat(response.getRiskScore()).isZero();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(response.getStatus()).isEqualTo(RiskStatus.APPROVED);
        assertThat(response.getTriggeredRule()).isEmpty();

        verify(repository, times(1)).save(any(RiskAssessment.class));
    }

    @Test
    void evaluateTransactionRisk_ShouldEvaluateCriticalRisk_WhenMultipleRulesTriggered() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        // High amount rule triggers (+40)
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .userId(userId)
                .transactionId(transactionId)
                .amount(new BigDecimal("200000"))
                .currency(CurrencyType.INR)
                .type(TransactionType.TRANSFER)
                .build();

        // 6 transfers in the last 2 minutes -> FrequentTransferRule triggers (+30)
        // 4 failed transfers -> FailedTransferRule triggers (+20)
        // Total score = 40 + 30 + 20 = 90 (CRITICAL)
        List<TransactionResponse> history = List.of(
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusMinutes(1)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusMinutes(2)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusMinutes(2)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusMinutes(3)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(3)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(4))
        );

        when(transactionClient.getTransactionHistory()).thenReturn(ApiResponse.success("success", history));
        when(auditClient.getUserAuditLogs(userId)).thenReturn(ApiResponse.success("success", List.of()));

        when(repository.save(any(RiskAssessment.class))).thenAnswer(invocation -> {
            RiskAssessment assessment = invocation.getArgument(0);
            assessment.setId(UUID.randomUUID());
            assessment.setCreatedAt(LocalDateTime.now());
            return assessment;
        });

        RiskResponse response = riskService.evaluateTransactionRisk(request);

        assertThat(response).isNotNull();
        assertThat(response.getRiskScore()).isEqualTo(90);
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(response.getStatus()).isEqualTo(RiskStatus.BLOCKED);
        assertThat(response.getTriggeredRule()).contains("HIGH_AMOUNT_RULE", "FREQUENT_TRANSFER_RULE", "FAILED_TRANSFER_RULE");
    }

    @Test
    void evaluateTransactionRisk_ShouldHandleFeignExceptionsGracefully() {
        UUID userId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .userId(userId)
                .amount(new BigDecimal("100"))
                .currency(CurrencyType.INR)
                .type(TransactionType.TRANSFER)
                .build();

        // Force Feign clients to throw exceptions
        when(transactionClient.getTransactionHistory()).thenThrow(new RuntimeException("Feign connection timed out"));
        when(auditClient.getUserAuditLogs(userId)).thenThrow(new RuntimeException("Audit service not reachable"));

        when(repository.save(any(RiskAssessment.class))).thenAnswer(invocation -> {
            RiskAssessment assessment = invocation.getArgument(0);
            assessment.setId(UUID.randomUUID());
            assessment.setCreatedAt(LocalDateTime.now());
            return assessment;
        });

        // The evaluation must complete successfully using default fallback lists
        RiskResponse response = riskService.evaluateTransactionRisk(request);

        assertThat(response).isNotNull();
        assertThat(response.getRiskScore()).isZero();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(response.getStatus()).isEqualTo(RiskStatus.APPROVED);
    }

    private TransactionResponse createTxn(TransactionType type, TransactionStatus status, LocalDateTime time) {
        return TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .type(type)
                .status(status)
                .timestamp(time)
                .amount(BigDecimal.TEN)
                .currency(CurrencyType.INR)
                .build();
    }
}
