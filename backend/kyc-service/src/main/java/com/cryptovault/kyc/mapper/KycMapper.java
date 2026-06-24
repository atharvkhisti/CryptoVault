package com.cryptovault.kyc.mapper;

import com.cryptovault.kyc.entity.KycDocument;
import com.cryptovault.kyc.entity.KycRecord;
import com.cryptovault.kyc.dto.response.DocumentResponse;
import com.cryptovault.kyc.dto.response.KycResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h3>KycMapper</h3>
 *
 * <p><b>Why it exists:</b> Decouples database entities from public REST response models, avoiding leakage of internal folder structures or file paths.</p>
 * <p><b>Architectural Layer:</b> Mapping Layer.</p>
 * <p><b>Design Patterns Used:</b> Data Mapper Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Ensures that internal file system pathways (file_path) are never exposed through public HTTP response payloads, mitigating system path disclosures.</p>
 */
@Component
public class KycMapper {

    /**
     * Map document entity to DTO representation.
     *
     * @param doc source document entity
     * @return DTO representation
     */
    public DocumentResponse toResponse(KycDocument doc) {
        if (doc == null) {
            return null;
        }
        return DocumentResponse.builder()
                .documentId(doc.getId())
                .documentType(doc.getDocumentType())
                .fileName(doc.getFileName())
                .mimeType(doc.getMimeType())
                .fileSize(doc.getFileSize())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }

    /**
     * Map KYC record entity and its associated documents to standard response DTO.
     *
     * @param record source record entity
     * @param documents associated documents list
     * @return unified response DTO
     */
    public KycResponse toResponse(KycRecord record, List<KycDocument> documents) {
        if (record == null) {
            return null;
        }
        List<DocumentResponse> docResponses = documents == null ? List.of() :
                documents.stream().map(this::toResponse).collect(Collectors.toList());

        return KycResponse.builder()
                .kycId(record.getId())
                .userId(record.getUserId())
                .status(record.getStatus())
                .remarks(record.getRemarks())
                .submittedAt(record.getSubmittedAt())
                .reviewedAt(record.getReviewedAt())
                .createdAt(record.getCreatedAt())
                .documents(docResponses)
                .build();
    }
}
