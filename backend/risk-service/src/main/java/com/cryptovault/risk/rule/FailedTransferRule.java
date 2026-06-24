package com.cryptovault.risk.rule;

import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.TransactionResponse;
import com.cryptovault.risk.dto.response.AuditResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h3>FailedTransferRule</h3>
 *
 * <p><b>Why it exists:</b> Rule strategy that flags accounts with excessive transfer failure rates (>3 failed transfers).</p>
 * <p><b>Architectural Layer:</b> Rule / Business Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern.</p>
 * <p><b>Banking Relevance:</b> High transaction failure rates indicate potential phishing attempts, card probing, or incorrect credentials entry sequences.</p>
 * <p><b>Scalability Considerations:</b> Optimized collection filtering metrics.</p>
 * <p><b>Interview Talking Points:</b> Scans the entire retrieved history logs matching transfer types and failed states.</p>
 */
@Component
public class FailedTransferRule implements RiskRule {

    @Override
    public int evaluate(EvaluateRiskRequest request, List<TransactionResponse> transactions, List<AuditResponse> auditLogs) {
        if (transactions == null || transactions.isEmpty()) {
            return 0;
        }

        long failedCount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER)
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .count();

        if (failedCount > 3) {
            return 20; // +20 points
        }
        return 0;
    }

    @Override
    public String getRuleName() {
        return "FAILED_TRANSFER_RULE";
    }
}
