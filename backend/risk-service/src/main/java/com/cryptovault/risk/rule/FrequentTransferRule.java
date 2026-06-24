package com.cryptovault.risk.rule;

import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.TransactionResponse;
import com.cryptovault.risk.dto.response.AuditResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h3>FrequentTransferRule</h3>
 *
 * <p><b>Why it exists:</b> Rule strategy that flags rapid/frequent transfers (>5 within 5 minutes).</p>
 * <p><b>Architectural Layer:</b> Rule / Business Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern.</p>
 * <p><b>Banking Relevance:</b> Protects against structural attacks, account takeover activity, or script-based velocity laundering.</p>
 * <p><b>Scalability Considerations:</b> Handled efficiently by filtering collections chronologically.</p>
 * <p><b>Interview Talking Points:</b> Compares transaction timestamps with local offset windows to calculate velocity metrics.</p>
 */
@Component
public class FrequentTransferRule implements RiskRule {

    @Override
    public int evaluate(EvaluateRiskRequest request, List<TransactionResponse> transactions, List<AuditResponse> auditLogs) {
        if (transactions == null || transactions.isEmpty()) {
            return 0;
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        long transferCount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER)
                .filter(t -> t.getTimestamp() != null && t.getTimestamp().isAfter(fiveMinutesAgo))
                .count();

        if (transferCount > 5) {
            return 30; // +30 points
        }
        return 0;
    }

    @Override
    public String getRuleName() {
        return "FREQUENT_TRANSFER_RULE";
    }
}
