package com.cryptovault.risk.rule;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.risk.dto.request.EvaluateRiskRequest;
import com.cryptovault.risk.dto.response.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RiskRuleTests {

    private final HighAmountRule highAmountRule = new HighAmountRule();
    private final FrequentTransferRule frequentTransferRule = new FrequentTransferRule();
    private final FailedTransferRule failedTransferRule = new FailedTransferRule();
    private final RapidWithdrawalRule rapidWithdrawalRule = new RapidWithdrawalRule();

    @Test
    void highAmountRule_ShouldTrigger_WhenAmountExceeds100kInr() {
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .amount(new BigDecimal("100001"))
                .currency(CurrencyType.INR)
                .build();

        int score = highAmountRule.evaluate(request, List.of(), List.of());
        assertThat(score).isEqualTo(40);
    }

    @Test
    void highAmountRule_ShouldTrigger_WhenConvertedAmountExceeds100kInr() {
        // 1200 USDT * 85 = 102,000 INR (> 100k INR)
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .amount(new BigDecimal("1200"))
                .currency(CurrencyType.USDT)
                .build();

        int score = highAmountRule.evaluate(request, List.of(), List.of());
        assertThat(score).isEqualTo(40);
    }

    @Test
    void highAmountRule_ShouldNotTrigger_WhenAmountBelow100kInr() {
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .amount(new BigDecimal("1000"))
                .currency(CurrencyType.USDT) // 1000 * 85 = 85,000 INR
                .build();

        int score = highAmountRule.evaluate(request, List.of(), List.of());
        assertThat(score).isZero();
    }

    @Test
    void frequentTransferRule_ShouldTrigger_WhenMoreThan5TransfersIn5Mins() {
        UUID userId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder().userId(userId).build();

        // 6 transfers within the last 2 minutes
        List<TransactionResponse> history = List.of(
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(1)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(2)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(2)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(3)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(3)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(4))
        );

        int score = frequentTransferRule.evaluate(request, history, List.of());
        assertThat(score).isEqualTo(30);
    }

    @Test
    void frequentTransferRule_ShouldNotTrigger_WhenLessThan5TransfersIn5Mins() {
        UUID userId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder().userId(userId).build();

        List<TransactionResponse> history = List.of(
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(1)),
                createTxn(TransactionType.DEPOSIT, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(2)), // Non-transfer
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(6)), // Older than 5m
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(2))
        );

        int score = frequentTransferRule.evaluate(request, history, List.of());
        assertThat(score).isZero();
    }

    @Test
    void failedTransferRule_ShouldTrigger_WhenMoreThan3FailedTransfers() {
        UUID userId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder().userId(userId).build();

        // 4 failed transfers (time is irrelevant for this rule)
        List<TransactionResponse> history = List.of(
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusDays(1)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusDays(2)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusDays(3)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusDays(4))
        );

        int score = failedTransferRule.evaluate(request, history, List.of());
        assertThat(score).isEqualTo(20);
    }

    @Test
    void failedTransferRule_ShouldNotTrigger_WhenLessThan3FailedTransfers() {
        UUID userId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder().userId(userId).build();

        List<TransactionResponse> history = List.of(
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusDays(1)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.COMPLETED, LocalDateTime.now().minusDays(2)),
                createTxn(TransactionType.TRANSFER, TransactionStatus.FAILED, LocalDateTime.now().minusDays(3))
        );

        int score = failedTransferRule.evaluate(request, history, List.of());
        assertThat(score).isZero();
    }

    @Test
    void rapidWithdrawalRule_ShouldTrigger_WhenWithdrawalWithin30MinsOfDeposit() {
        UUID userId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .userId(userId)
                .type(TransactionType.WITHDRAW)
                .build();

        List<TransactionResponse> history = List.of(
                createTxn(TransactionType.DEPOSIT, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(10))
        );

        int score = rapidWithdrawalRule.evaluate(request, history, List.of());
        assertThat(score).isEqualTo(25);
    }

    @Test
    void rapidWithdrawalRule_ShouldNotTrigger_WhenDepositOlderThan30MinsOrFailed() {
        UUID userId = UUID.randomUUID();
        EvaluateRiskRequest request = EvaluateRiskRequest.builder()
                .userId(userId)
                .type(TransactionType.WITHDRAW)
                .build();

        List<TransactionResponse> history = List.of(
                createTxn(TransactionType.DEPOSIT, TransactionStatus.FAILED, LocalDateTime.now().minusMinutes(10)), // Failed
                createTxn(TransactionType.DEPOSIT, TransactionStatus.COMPLETED, LocalDateTime.now().minusMinutes(35)) // Too old
        );

        int score = rapidWithdrawalRule.evaluate(request, history, List.of());
        assertThat(score).isZero();
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
