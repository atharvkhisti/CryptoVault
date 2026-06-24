package com.cryptovault.risk.service;

import com.cryptovault.common.enums.RiskLevel;
import com.cryptovault.common.enums.RiskStatus;
import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.risk.client.AuditClient;
import com.cryptovault.risk.client.TransactionClient;
import com.cryptovault.risk.client.WalletClient;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.AuditResponse;
import com.cryptovault.risk.dto.response.RiskResponse;
import com.cryptovault.risk.dto.response.TransactionResponse;
import com.cryptovault.risk.dto.response.WalletResponse;
import com.cryptovault.risk.entity.RiskAssessment;
import com.cryptovault.risk.exception.RiskAssessmentNotFoundException;
import com.cryptovault.risk.mapper.RiskMapper;
import com.cryptovault.risk.repository.RiskAssessmentRepository;
import com.cryptovault.risk.rule.RiskRule;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <h3>RiskService</h3>
 *
 * <p><b>Why it exists:</b> Coordinates modular risk evaluations, aggregates rule scores, maps levels/status, and writes compliance assessments.</p>
 * <p><b>Architectural Layer:</b> Service / Business Logic Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern (Rule collection execution), Builder Pattern.</p>
 * <p><b>Banking Relevance:</b> Central engine protecting bank assets and preventing fraud by evaluating rules before completing money movements.</p>
 * <p><b>Scalability Considerations:</b> Gracefully handles downstream client failures via catch-blocks falling back to safe defaults, preventing cascading failures.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Injecting a list of rules (<code>List<RiskRule></code>) leverages Spring dependency injection to satisfy Open/Closed principles.
 * 2. Increments Prometheus metrics using standard Micrometer <code>MeterRegistry</code> counters.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskService {

    private final RiskAssessmentRepository repository;
    private final RiskMapper mapper;
    private final TransactionClient transactionClient;
    private final AuditClient auditClient;
    private final WalletClient walletClient;
    private final List<RiskRule> rules;
    private final MeterRegistry meterRegistry;

    /**
     * Evaluates the risk associated with a specific transaction request.
     *
     * @param request target transaction details
     * @return evaluated RiskResponse DTO
     */
    @Transactional
    public RiskResponse evaluateTransactionRisk(EvaluateRiskRequest request) {
        log.info("Evaluating transaction risk for user={} transaction={}", request.getUserId(), request.getTransactionId());

        // Fetch downstream context with resilience fallbacks
        List<TransactionResponse> transactionHistory = fetchTransactionHistorySafely();
        List<AuditResponse> auditLogs = fetchAuditLogsSafely(request.getUserId());

        // Calculate score
        int score = calculateRiskScore(request, transactionHistory, auditLogs);
        RiskLevel level = mapRiskLevel(score);
        RiskStatus status = mapRiskStatus(level);

        // Build triggered rules comment
        String triggeredRules = getTriggeredRulesDescription(request, transactionHistory, auditLogs);
        String comments = triggeredRules.isEmpty() ? "No rules triggered. Transaction approved." : "Rules triggered: " + triggeredRules;

        // Persist assessment
        RiskAssessment assessment = RiskAssessment.builder()
                .userId(request.getUserId())
                .transactionId(request.getTransactionId())
                .riskScore(score)
                .riskLevel(level)
                .status(status)
                .triggeredRule(triggeredRules)
                .comments(comments)
                .build();

        RiskAssessment saved = repository.save(assessment);

        // Telemetry incrementing
        incrementMetrics(level, status);

        return mapper.toResponse(saved);
    }

    /**
     * Evaluates a user's general historical risk profile.
     *
     * @param userId user identifier
     * @return general user RiskResponse
     */
    @Transactional
    public RiskResponse evaluateUserRisk(UUID userId) {
        log.info("Evaluating user-only risk profile for user={}", userId);
        EvaluateRiskRequest dummyRequest = EvaluateRiskRequest.builder()
                .userId(userId)
                .amount(BigDecimal.ZERO)
                .currency(CurrencyType.INR)
                .type(TransactionType.TRANSFER)
                .build();

        return evaluateTransactionRisk(dummyRequest);
    }

    /**
     * Aggregates the risk score by running all autowired rules.
     *
     * @param request target request details
     * @param transactions transaction logs history
     * @param auditLogs audit logs history
     * @return total aggregated risk score
     */
    public int calculateRiskScore(EvaluateRiskRequest request, List<TransactionResponse> transactions, List<AuditResponse> auditLogs) {
        int totalScore = 0;
        for (RiskRule rule : rules) {
            totalScore += rule.evaluate(request, transactions, auditLogs);
        }
        return Math.min(totalScore, 100);
    }

    /**
     * Retrieve a specific risk assessment by ID.
     *
     * @param id assessment identifier
     * @return matched RiskResponse
     */
    @Transactional(readOnly = true)
    public RiskResponse getRiskAssessment(UUID id) {
        RiskAssessment assessment = repository.findById(id)
                .orElseThrow(() -> new RiskAssessmentNotFoundException("Risk assessment not found for ID: " + id));
        return mapper.toResponse(assessment);
    }

    /**
     * Retrieve risk assessments history for a specific user.
     *
     * @param userId user identifier
     * @return list of risk responses
     */
    @Transactional(readOnly = true)
    public List<RiskResponse> getUserRiskHistory(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all risk assessments registered in the system.
     *
     * @return list of all risk assessments
     */
    @Transactional(readOnly = true)
    public List<RiskResponse> getAllAssessments() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    private List<TransactionResponse> fetchTransactionHistorySafely() {
        try {
            var apiResponse = transactionClient.getTransactionHistory();
            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                return apiResponse.getData();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch transaction history from Transaction Service via Feign, falling back to empty list. Error: {}", ex.getMessage());
        }
        return new ArrayList<>();
    }

    private List<AuditResponse> fetchAuditLogsSafely(UUID userId) {
        try {
            var apiResponse = auditClient.getUserAuditLogs(userId);
            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                return apiResponse.getData();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch audit logs from Audit Service via Feign, falling back to empty list. Error: {}", ex.getMessage());
        }
        return new ArrayList<>();
    }

    private RiskLevel mapRiskLevel(int score) {
        if (score <= 25) return RiskLevel.LOW;
        if (score <= 50) return RiskLevel.MEDIUM;
        if (score <= 75) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    private RiskStatus mapRiskStatus(RiskLevel level) {
        switch (level) {
            case HIGH:
                return RiskStatus.FLAGGED;
            case CRITICAL:
                return RiskStatus.BLOCKED;
            case MEDIUM:
            case LOW:
            default:
                return RiskStatus.APPROVED;
        }
    }

    private String getTriggeredRulesDescription(EvaluateRiskRequest request, List<TransactionResponse> transactions, List<AuditResponse> auditLogs) {
        List<String> triggered = new ArrayList<>();
        for (RiskRule rule : rules) {
            int ruleScore = rule.evaluate(request, transactions, auditLogs);
            if (ruleScore > 0) {
                triggered.add(rule.getRuleName() + "(+" + ruleScore + ")");
            }
        }
        return String.join(", ", triggered);
    }

    private void incrementMetrics(RiskLevel level, RiskStatus status) {
        try {
            meterRegistry.counter("risk_evaluations_total", "riskLevel", level.name(), "status", status.name()).increment();
            if (level == RiskLevel.HIGH) {
                meterRegistry.counter("high_risk_transactions").increment();
            } else if (level == RiskLevel.CRITICAL) {
                meterRegistry.counter("critical_risk_transactions").increment();
            }
            if (status == RiskStatus.FLAGGED || status == RiskStatus.BLOCKED) {
                meterRegistry.counter("flagged_accounts").increment();
            }
        } catch (Exception ex) {
            log.warn("Failed to increment observability metrics: {}", ex.getMessage());
        }
    }
}
