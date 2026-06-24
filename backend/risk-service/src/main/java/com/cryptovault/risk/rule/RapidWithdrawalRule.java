package com.cryptovault.risk.rule;

import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.TransactionResponse;
import com.cryptovault.risk.dto.response.AuditResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h3>RapidWithdrawalRule</h3>
 *
 * <p><b>Why it exists:</b> Rule strategy that flags rapid withdrawals initiated shortly after a completed deposit.</p>
 * <p><b>Architectural Layer:</b> Rule / Business Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern.</p>
 * <p><b>Banking Relevance:</b> Protects against check-cashing fraud, automated carding schemes, or instant cash-outs of stolen deposits.</p>
 * <p><b>Scalability Considerations:</b> Handled efficiently via lazy evaluations checks.</p>
 * <p><b>Interview Talking Points:</b> Triggers if the current request is a WITHDRAW and the transaction history shows a completed DEPOSIT in the last 30 minutes.</p>
 */
@Component
public class RapidWithdrawalRule implements RiskRule {

    @Override
    public int evaluate(EvaluateRiskRequest request, List<TransactionResponse> transactions, List<AuditResponse> auditLogs) {
        if (request == null || request.getType() != TransactionType.WITHDRAW) {
            return 0;
        }

        if (transactions == null || transactions.isEmpty()) {
            return 0;
        }

        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        boolean hasRecentDeposit = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .anyMatch(t -> t.getTimestamp() != null && t.getTimestamp().isAfter(thirtyMinutesAgo));

        if (hasRecentDeposit) {
            return 25; // +25 points
        }
        return 0;
    }

    @Override
    public String getRuleName() {
        return "RAPID_WITHDRAWAL_RULE";
    }
}
