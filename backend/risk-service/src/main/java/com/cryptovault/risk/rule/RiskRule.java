package com.cryptovault.risk.rule;

import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.TransactionResponse;
import com.cryptovault.risk.dto.response.AuditResponse;

import java.util.List;

/**
 * <h3>RiskRule</h3>
 *
 * <p><b>Why it exists:</b> Strategy interface defining risk evaluation contracts for the Rule Engine.</p>
 * <p><b>Architectural Layer:</b> Rule / Business Domain Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern.</p>
 * <p><b>Banking Relevance:</b> Enables evaluation of localized compliance checks on transactions before authorization approval.</p>
 * <p><b>Scalability Considerations:</b> Extremely scalable; running in-memory checks over pre-loaded collections avoids database access overhead.</p>
 * <p><b>Interview Talking Points:</b> Serves as the strategy context contract in the Strategy Pattern, complying with the Open/Closed Principle.</p>
 */
public interface RiskRule {

    /**
     * Evaluates a risk rule against the target request and historical context.
     *
     * @param request the risk assessment evaluation details
     * @param transactions historical transaction logs for the user
     * @param auditLogs historical audit logs for the user
     * @return risk points contribution (0 if not triggered)
     */
    int evaluate(EvaluateRiskRequest request, List<TransactionResponse> transactions, List<AuditResponse> auditLogs);

    /**
     * Get the identifying code name of this rule.
     *
     * @return rule name
     */
    String getRuleName();
}
