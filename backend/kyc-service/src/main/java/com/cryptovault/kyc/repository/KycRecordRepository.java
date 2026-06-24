package com.cryptovault.kyc.repository;

import com.cryptovault.kyc.entity.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * <h3>KycRecordRepository</h3>
 *
 * <p><b>Why it exists:</b> Repository layer to query and persist {@link KycRecord} entities.</p>
 * <p><b>Architectural Layer:</b> Persistence Repository Layer.</p>
 * <p><b>Design Patterns Used:</b> Data Access Object (DAO) / Repository Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Direct interface to load KYC status during user logins or transaction risk checking.</p>
 */
@Repository
public interface KycRecordRepository extends JpaRepository<KycRecord, UUID> {

    /**
     * Query the KYC record associated with a user ID.
     *
     * @param userId user identifier
     * @return optional KYC record
     */
    Optional<KycRecord> findByUserId(UUID userId);
}
