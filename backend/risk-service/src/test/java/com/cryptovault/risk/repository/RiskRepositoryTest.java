package com.cryptovault.risk.repository;

import com.cryptovault.common.enums.RiskLevel;
import com.cryptovault.common.enums.RiskStatus;
import com.cryptovault.risk.entity.RiskAssessment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RiskRepositoryTest {

    @Autowired
    private RiskAssessmentRepository repository;

    @Test
    void shouldSaveAndQueryAssessmentsCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        RiskAssessment assessment = RiskAssessment.builder()
                .userId(userId)
                .transactionId(transactionId)
                .riskScore(40)
                .riskLevel(RiskLevel.MEDIUM)
                .status(RiskStatus.APPROVED)
                .triggeredRule("HIGH_AMOUNT_RULE")
                .comments("Triggered high amount rule")
                .createdAt(LocalDateTime.now())
                .build();

        RiskAssessment saved = repository.save(assessment);
        assertThat(saved.getId()).isNotNull();

        List<RiskAssessment> byUser = repository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(byUser).hasSize(1);
        assertThat(byUser.get(0).getRiskScore()).isEqualTo(40);

        var byTransaction = repository.findByTransactionId(transactionId);
        assertThat(byTransaction).isPresent();
        assertThat(byTransaction.get().getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }
}
