package com.cryptovault.risk.rule;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.TransactionResponse;
import com.cryptovault.risk.dto.response.AuditResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h3>HighAmountRule</h3>
 *
 * <p><b>Why it exists:</b> Rule strategy that flags high-value transactions exceeding ₹100,000 INR.</p>
 * <p><b>Architectural Layer:</b> Rule / Business Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern.</p>
 * <p><b>Banking Relevance:</b> Flags large money movements which require additional AML approvals to prevent compliance violations.</p>
 * <p><b>Scalability Considerations:</b> Handled via quick mathematical comparisons locally in memory.</p>
 * <p><b>Interview Talking Points:</b> Uses a hardcoded exchange-rate conversion matrix to translate BTC, ETH, and USDT into INR for evaluation.</p>
 */
@Component
public class HighAmountRule implements RiskRule {

    private static final BigDecimal INR_LIMIT = new BigDecimal("100000");

    @Override
    public int evaluate(EvaluateRiskRequest request, List<TransactionResponse> transactions, List<AuditResponse> auditLogs) {
        if (request == null || request.getAmount() == null || request.getCurrency() == null) {
            return 0;
        }

        BigDecimal amountInInr = convertToInr(request.getAmount(), request.getCurrency());
        if (amountInInr.compareTo(INR_LIMIT) > 0) {
            return 40; // +40 points
        }
        return 0;
    }

    @Override
    public String getRuleName() {
        return "HIGH_AMOUNT_RULE";
    }

    private BigDecimal convertToInr(BigDecimal amount, CurrencyType currency) {
        BigDecimal rate;
        switch (currency) {
            case USDT:
                rate = new BigDecimal("85");
                break;
            case BTC:
                rate = new BigDecimal("5000000");
                break;
            case ETH:
                rate = new BigDecimal("300000");
                break;
            case INR:
            default:
                rate = BigDecimal.ONE;
                break;
        }
        return amount.multiply(rate);
    }
}
