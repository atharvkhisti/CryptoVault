package com.cryptovault.kyc.entity;

import com.cryptovault.common.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>KycDocument</h3>
 *
 * <p><b>Why it exists:</b> JPA entity mapping to uploaded identity document metadata.</p>
 * <p><b>Architectural Layer:</b> Persistence Layer / Entity.</p>
 * <p><b>Design Patterns Used:</b> Active Record / Table Mapping Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Persists physical file audit trails (names, sizes, types, and paths) to comply with data conservation requirements.</p>
 * <p><b>Scalability Considerations:</b> Indexes on kyc_record_id speed up checks verifying if required docs are present before submission.</p>
 * <p><b>Interview Talking Points:</b> Stores documents' logical references while delegates structural streaming/saving to strategy-configured Storage services.</p>
 */
@Entity
@Table(name = "kyc_documents", indexes = {
    @Index(name = "idx_kyc_documents_record_id", columnList = "kyc_record_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "kyc_record_id", nullable = false)
    private UUID kycRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
