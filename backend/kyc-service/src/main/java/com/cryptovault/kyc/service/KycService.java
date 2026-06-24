package com.cryptovault.kyc.service;

import com.cryptovault.common.enums.AuditEventType;
import com.cryptovault.common.enums.DocumentType;
import com.cryptovault.common.enums.KycStatus;
import com.cryptovault.common.enums.NotificationType;
import com.cryptovault.kyc.client.AuditClient;
import com.cryptovault.kyc.client.AuthClient;
import com.cryptovault.kyc.client.NotificationClient;
import com.cryptovault.kyc.dto.response.KycResponse;
import com.cryptovault.kyc.entity.KycDocument;
import com.cryptovault.kyc.entity.KycRecord;
import com.cryptovault.kyc.exception.KycException;
import com.cryptovault.kyc.exception.KycRecordNotFoundException;
import com.cryptovault.kyc.mapper.KycMapper;
import com.cryptovault.kyc.repository.KycDocumentRepository;
import com.cryptovault.kyc.repository.KycRecordRepository;
import com.cryptovault.kyc.validation.DocumentValidator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h3>KycService</h3>
 *
 * <p><b>Why it exists:</b> Coordinates KYC business validations, registers document records, evaluates upload bounds, and writes status logs.</p>
 * <p><b>Architectural Layer:</b> Service / Business Logic Layer.</p>
 * <p><b>Design Patterns Used:</b> Strategy Pattern (calling the dynamic StorageService interface), Builder Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Direct enforcement point checking that unverified users are flagged and blocked from moving money, and logs events for auditing.</p>
 * <p><b>Scalability Considerations:</b> Gracefully handles downstream client failures via catch-blocks falling back to safe defaults, avoiding cascading system outages.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Uses the Strategy Pattern to decouple logical uploads from physical storage (Local vs S3).
 * 2. Increments Prometheus metrics using standard Micrometer <code>MeterRegistry</code> counters.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final KycRecordRepository recordRepository;
    private final KycDocumentRepository documentRepository;
    private final StorageService storageService;
    private final DocumentValidator documentValidator;
    private final KycMapper mapper;
    private final AuthClient authClient;
    private final AuditClient auditClient;
    private final NotificationClient notificationClient;
    private final MeterRegistry meterRegistry;

    /**
     * Creates or resets a KYC request for a user.
     *
     * @param userId user identifier
     * @return initialized KycResponse DTO
     */
    @Transactional
    public KycResponse createKycRequest(UUID userId) {
        log.info("Initializing KYC record creation request for user={}", userId);

        // Verify user existence via Auth Service
        try {
            var userResponse = authClient.getUserById(userId);
            if (userResponse == null || !userResponse.isSuccess() || userResponse.getData() == null) {
                throw new KycException("User does not exist in Auth Service", com.cryptovault.common.enums.ErrorCode.USER_NOT_FOUND);
            }
        } catch (KycException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to verify user existence via Auth Service Feign client: {}", ex.getMessage());
            throw new KycException("User verification failed or Auth Service is unreachable", com.cryptovault.common.enums.ErrorCode.USER_NOT_FOUND);
        }

        Optional<KycRecord> existingRecordOpt = recordRepository.findByUserId(userId);
        if (existingRecordOpt.isPresent()) {
            KycRecord existingRecord = existingRecordOpt.get();
            if (existingRecord.getStatus() == KycStatus.APPROVED || existingRecord.getStatus() == KycStatus.UNDER_REVIEW) {
                throw new KycException("KYC record is already APPROVED or UNDER_REVIEW", com.cryptovault.common.enums.ErrorCode.INVALID_KYC_STATUS);
            }
            
            // For PENDING, REJECTED, or EXPIRED, reset back to mutable PENDING status
            if (existingRecord.getStatus() != KycStatus.PENDING) {
                existingRecord.setStatus(KycStatus.PENDING);
                existingRecord.setRemarks(null);
                existingRecord.setSubmittedAt(null);
                existingRecord.setReviewedAt(null);
                recordRepository.save(existingRecord);
            }
            return mapper.toResponse(existingRecord, documentRepository.findByKycRecordId(existingRecord.getId()));
        }

        KycRecord record = KycRecord.builder()
                .userId(userId)
                .status(KycStatus.PENDING)
                .build();
        KycRecord savedRecord = recordRepository.save(record);
        log.info("Initialized new KYC record in PENDING state for user={}", userId);
        return mapper.toResponse(savedRecord, List.of());
    }

    /**
     * Uploads and associates a document type to a user's KYC record.
     *
     * @param userId user identifier
     * @param file uploaded document file
     * @param documentType type of document
     * @return updated KycResponse DTO
     */
    @Transactional
    public KycResponse uploadDocument(UUID userId, MultipartFile file, DocumentType documentType) {
        log.info("Uploading document type={} for user={}", documentType, userId);
        documentValidator.validate(file);

        KycRecord record = recordRepository.findByUserId(userId)
                .orElseThrow(() -> new KycRecordNotFoundException("KYC record not initialized. Please call POST /api/kyc first"));

        if (record.getStatus() == KycStatus.APPROVED || record.getStatus() == KycStatus.UNDER_REVIEW) {
            throw new KycException("Cannot upload documents when KYC status is " + record.getStatus(), com.cryptovault.common.enums.ErrorCode.INVALID_KYC_STATUS);
        }

        // Handle duplicate upload by overwrite
        Optional<KycDocument> existingDocOpt = documentRepository.findByKycRecordIdAndDocumentType(record.getId(), documentType);
        if (existingDocOpt.isPresent()) {
            KycDocument existingDoc = existingDocOpt.get();
            try {
                storageService.delete(existingDoc.getFilePath());
            } catch (Exception e) {
                log.warn("Failed to delete overwritten file from physical storage: {}", e.getMessage());
            }
            documentRepository.delete(existingDoc);
            log.info("Deleted previous duplicate document type={} for user={}", documentType, userId);
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String fileName = userId + "_" + documentType + "_" + UUID.randomUUID() + fileExtension;

        String storedPath;
        try {
            storedPath = storageService.store(file, userId.toString(), fileName);
        } catch (IOException e) {
            log.error("Failed to store file via strategy: {}", e.getMessage());
            throw new KycException("Failed to save uploaded file: " + e.getMessage(), com.cryptovault.common.enums.ErrorCode.INTERNAL_SERVER_ERROR);
        }

        KycDocument document = KycDocument.builder()
                .kycRecordId(record.getId())
                .documentType(documentType)
                .fileName(file.getOriginalFilename())
                .filePath(storedPath)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .build();
        documentRepository.save(document);

        incrementUploadMetrics();

        return mapper.toResponse(record, documentRepository.findByKycRecordId(record.getId()));
    }

    /**
     * Submits KYC files for compliance review.
     *
     * @param userId user identifier
     * @return submitted KycResponse DTO
     */
    @Transactional
    public KycResponse submitForReview(UUID userId) {
        log.info("Submitting KYC documents for review for user={}", userId);

        KycRecord record = recordRepository.findByUserId(userId)
                .orElseThrow(() -> new KycRecordNotFoundException("KYC record not initialized for user: " + userId));

        if (record.getStatus() == KycStatus.APPROVED || record.getStatus() == KycStatus.UNDER_REVIEW) {
            throw new KycException("KYC is already APPROVED or UNDER_REVIEW", com.cryptovault.common.enums.ErrorCode.INVALID_KYC_STATUS);
        }

        List<KycDocument> documents = documentRepository.findByKycRecordId(record.getId());
        boolean hasPan = documents.stream().anyMatch(d -> d.getDocumentType() == DocumentType.PAN);
        boolean hasAadhaar = documents.stream().anyMatch(d -> d.getDocumentType() == DocumentType.AADHAAR);

        if (!hasPan || !hasAadhaar) {
            throw new KycException("Mandatory documents (PAN and AADHAAR) are missing", com.cryptovault.common.enums.ErrorCode.MANDATORY_DOCUMENTS_MISSING);
        }

        record.setStatus(KycStatus.UNDER_REVIEW);
        record.setSubmittedAt(LocalDateTime.now());
        recordRepository.save(record);

        logAuditEventSafely(userId, AuditEventType.KYC_SUBMITTED, "KYC_SUBMIT", "KYC documents submitted for review");
        incrementSubmissionMetrics();

        return mapper.toResponse(record, documents);
    }

    /**
     * Approves a KYC record (Admin clearance).
     *
     * @param kycId KYC record UUID
     * @return approved KycResponse DTO
     */
    @Transactional
    public KycResponse approveKyc(UUID kycId) {
        log.info("Approving KYC record ID={}", kycId);

        KycRecord record = recordRepository.findById(kycId)
                .orElseThrow(() -> new KycRecordNotFoundException("KYC record not found for ID: " + kycId));

        if (record.getStatus() != KycStatus.UNDER_REVIEW) {
            throw new KycException("KYC record is not under review. Current status: " + record.getStatus(), com.cryptovault.common.enums.ErrorCode.INVALID_KYC_STATUS);
        }

        record.setStatus(KycStatus.APPROVED);
        record.setReviewedAt(LocalDateTime.now());
        record.setRemarks(null);
        recordRepository.save(record);

        logAuditEventSafely(record.getUserId(), AuditEventType.KYC_APPROVED, "KYC_APPROVE", "KYC record approved");
        sendNotificationSafely(record.getUserId(), NotificationType.KYC_APPROVED, "KYC Verification Approved",
                "Dear user, your KYC verification has been successful. You can now perform transactions on CryptoVault.");
        incrementApprovalMetrics();

        return mapper.toResponse(record, documentRepository.findByKycRecordId(record.getId()));
    }

    /**
     * Rejects a KYC record (Admin clearance).
     *
     * @param kycId KYC record UUID
     * @param remarks reason description
     * @return rejected KycResponse DTO
     */
    @Transactional
    public KycResponse rejectKyc(UUID kycId, String remarks) {
        log.info("Rejecting KYC record ID={} remarks={}", kycId, remarks);

        if (remarks == null || remarks.trim().isEmpty()) {
            throw new KycException("Rejection remarks must not be blank", com.cryptovault.common.enums.ErrorCode.INTERNAL_SERVER_ERROR);
        }

        KycRecord record = recordRepository.findById(kycId)
                .orElseThrow(() -> new KycRecordNotFoundException("KYC record not found for ID: " + kycId));

        if (record.getStatus() != KycStatus.UNDER_REVIEW) {
            throw new KycException("KYC record is not under review. Current status: " + record.getStatus(), com.cryptovault.common.enums.ErrorCode.INVALID_KYC_STATUS);
        }

        record.setStatus(KycStatus.REJECTED);
        record.setReviewedAt(LocalDateTime.now());
        record.setRemarks(remarks);
        recordRepository.save(record);

        logAuditEventSafely(record.getUserId(), AuditEventType.KYC_REJECTED, "KYC_REJECT", "KYC record rejected. Reason: " + remarks);
        sendNotificationSafely(record.getUserId(), NotificationType.KYC_REJECTED, "KYC Verification Rejected",
                "Dear user, your KYC verification has been rejected. Reason: " + remarks);
        incrementRejectionMetrics();

        return mapper.toResponse(record, documentRepository.findByKycRecordId(record.getId()));
    }

    /**
     * Queries KYC status details for a user ID.
     *
     * @param userId user identifier
     * @return KycResponse DTO
     */
    @Transactional(readOnly = true)
    public KycResponse getKycStatus(UUID userId) {
        KycRecord record = recordRepository.findByUserId(userId)
                .orElseThrow(() -> new KycRecordNotFoundException("KYC record not initialized for user ID: " + userId));
        List<KycDocument> documents = documentRepository.findByKycRecordId(record.getId());
        return mapper.toResponse(record, documents);
    }

    /**
     * Queries details of a KYC record by UUID.
     *
     * @param kycId record ID
     * @return KycResponse DTO
     */
    @Transactional(readOnly = true)
    public KycResponse getUserKyc(UUID kycId) {
        KycRecord record = recordRepository.findById(kycId)
                .orElseThrow(() -> new KycRecordNotFoundException("KYC record not found for ID: " + kycId));
        List<KycDocument> documents = documentRepository.findByKycRecordId(record.getId());
        return mapper.toResponse(record, documents);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".bin";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private void logAuditEventSafely(UUID userId, AuditEventType eventType, String action, String description) {
        try {
            var request = AuditClient.AuditEventRequest.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .serviceName("kyc-service")
                    .action(action)
                    .description(description)
                    .ipAddress("127.0.0.1")
                    .build();
            auditClient.logEvent(request);
        } catch (Exception ex) {
            log.warn("Failed to dispatch audit log via Feign client: {}", ex.getMessage());
        }
    }

    private void sendNotificationSafely(UUID userId, NotificationType type, String subject, String message) {
        try {
            String email = "user@cryptovault.com";
            try {
                var userResponse = authClient.getUserById(userId);
                if (userResponse != null && userResponse.isSuccess() && userResponse.getData() != null) {
                    email = userResponse.getData().getEmail();
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve user email for notification routing: {}", e.getMessage());
            }

            var request = NotificationClient.SendNotificationRequest.builder()
                    .userId(userId)
                    .email(email)
                    .type(type)
                    .subject(subject)
                    .message(message)
                    .build();
            notificationClient.sendNotification(request);
        } catch (Exception ex) {
            log.warn("Failed to dispatch notification via Feign client: {}", ex.getMessage());
        }
    }

    private void incrementUploadMetrics() {
        try {
            meterRegistry.counter("document_uploads_total").increment();
        } catch (Exception e) {
            log.warn("Failed to record Prometheus metrics: {}", e.getMessage());
        }
    }

    private void incrementSubmissionMetrics() {
        try {
            meterRegistry.counter("kyc_submissions_total").increment();
        } catch (Exception e) {
            log.warn("Failed to record Prometheus metrics: {}", e.getMessage());
        }
    }

    private void incrementApprovalMetrics() {
        try {
            meterRegistry.counter("kyc_approvals_total").increment();
        } catch (Exception e) {
            log.warn("Failed to record Prometheus metrics: {}", e.getMessage());
        }
    }

    private void incrementRejectionMetrics() {
        try {
            meterRegistry.counter("kyc_rejections_total").increment();
        } catch (Exception e) {
            log.warn("Failed to record Prometheus metrics: {}", e.getMessage());
        }
    }
}
