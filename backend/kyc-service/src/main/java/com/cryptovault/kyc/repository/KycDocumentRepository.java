package com.cryptovault.kyc.repository;

import com.cryptovault.common.enums.DocumentType;
import com.cryptovault.kyc.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h3>KycDocumentRepository</h3>
 *
 * <p><b>Why it exists:</b> Repository layer to query and persist {@link KycDocument} metadata.</p>
 * <p><b>Architectural Layer:</b> Persistence Repository Layer.</p>
 * <p><b>Design Patterns Used:</b> Data Access Object (DAO) / Repository Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Allows checks for duplicate document uploads or verifying mandatory compliance document sets prior to review.</p>
 */
@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    /**
     * Query all documents linked to a specific KYC record.
     *
     * @param kycRecordId the KYC record ID
     * @return list of matching documents
     */
    List<KycDocument> findByKycRecordId(UUID kycRecordId);

    /**
     * Query a specific document type linked to a KYC record.
     *
     * @param kycRecordId the KYC record ID
     * @param documentType the type of document
     * @return optional KYC document
     */
    Optional<KycDocument> findByKycRecordIdAndDocumentType(UUID kycRecordId, DocumentType documentType);
}
