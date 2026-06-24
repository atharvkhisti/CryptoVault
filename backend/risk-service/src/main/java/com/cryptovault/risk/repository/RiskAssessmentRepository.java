package com.cryptovault.risk.repository;

import com.cryptovault.risk.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h3>RiskAssessmentRepository</h3>
 *
 * <p><b>Why it exists:</b> Data access layer for retrieving and saving risk assessment entities.</p>
 * <p><b>Architectural Layer:</b> Persistence / Repository Layer.</p>
 * <p><b>Design Patterns Used:</b> Repository Pattern (Spring Data JPA abstraction).</p>
 * <p><b>Banking Relevance:</b> Enables compliance administrators to retrieve risk records by user profile or specific transactional ledger entries.</p>
 * <p><b>Scalability Considerations:</b> Optimized index lookup queries over indexed user_id and transaction_id fields prevent table scans.</p>
 * <p><b>Interview Talking Points:</b> Inherits from <code>JpaRepository</code>, providing out-of-the-box transaction management and CRUD features with custom query derivation.</p>
 */
@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    /**
     * Retrieve risk assessments associated with a user, sorted chronologically (newest first).
     *
     * @param userId user identifier
     * @return list of risk assessments
     */
    List<RiskAssessment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Retrieve a risk assessment associated with a specific transaction.
     *
     * @param transactionId transaction identifier
     * @return an Optional containing the risk assessment, if found
     */
    Optional<RiskAssessment> findByTransactionId(UUID transactionId);
}
