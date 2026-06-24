package com.cryptovault.kyc.service;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.DocumentType;
import com.cryptovault.common.enums.KycStatus;
import com.cryptovault.kyc.client.AuditClient;
import com.cryptovault.kyc.client.AuthClient;
import com.cryptovault.kyc.client.NotificationClient;
import com.cryptovault.kyc.dto.response.KycResponse;
import com.cryptovault.kyc.dto.response.UserResponse;
import com.cryptovault.kyc.entity.KycDocument;
import com.cryptovault.kyc.entity.KycRecord;
import com.cryptovault.kyc.exception.KycException;
import com.cryptovault.kyc.exception.KycRecordNotFoundException;
import com.cryptovault.kyc.mapper.KycMapper;
import com.cryptovault.kyc.repository.KycDocumentRepository;
import com.cryptovault.kyc.repository.KycRecordRepository;
import com.cryptovault.kyc.validation.DocumentValidator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Service layer tests verifying business validations and lifecycle triggers of KycService.
 */
@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    private KycService kycService;

    @Mock
    private KycRecordRepository recordRepository;

    @Mock
    private KycDocumentRepository documentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentValidator documentValidator;

    private KycMapper mapper;

    @Mock
    private AuthClient authClient;

    @Mock
    private AuditClient auditClient;

    @Mock
    private NotificationClient notificationClient;

    private MeterRegistry meterRegistry;

    private UUID userId;
    private KycRecord kycRecord;

    @BeforeEach
    void setUp() {
        mapper = new KycMapper();
        meterRegistry = new SimpleMeterRegistry();
        kycService = new KycService(
                recordRepository,
                documentRepository,
                storageService,
                documentValidator,
                mapper,
                authClient,
                auditClient,
                notificationClient,
                meterRegistry
        );
        userId = UUID.randomUUID();
        kycRecord = KycRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(KycStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateKycRequestSuccess() {
        when(authClient.getUserById(userId)).thenReturn(ApiResponse.success("Success", UserResponse.builder().id(userId).build()));
        when(recordRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(recordRepository.save(any(KycRecord.class))).thenAnswer(invocation -> {
            KycRecord r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        KycResponse response = kycService.createKycRequest(userId);
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(KycStatus.PENDING, response.getStatus());
    }

    @Test
    void testCreateKycRequestUserNotFound() {
        when(authClient.getUserById(userId)).thenReturn(ApiResponse.error("Not Found"));

        assertThrows(KycException.class, () -> kycService.createKycRequest(userId));
    }

    @Test
    void testUploadDocumentSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "pan.jpg", "image/jpeg", "bytes".getBytes());
        when(recordRepository.findByUserId(userId)).thenReturn(Optional.of(kycRecord));
        when(documentRepository.findByKycRecordIdAndDocumentType(kycRecord.getId(), DocumentType.PAN)).thenReturn(Optional.empty());
        when(storageService.store(any(), any(), any())).thenReturn("/path/to/pan.jpg");

        KycResponse response = kycService.uploadDocument(userId, file, DocumentType.PAN);
        assertNotNull(response);
        verify(storageService).store(any(), any(), any());
        verify(documentRepository).save(any(KycDocument.class));
    }

    @Test
    void testSubmitForReviewMissingMandatory() {
        when(recordRepository.findByUserId(userId)).thenReturn(Optional.of(kycRecord));
        when(documentRepository.findByKycRecordId(kycRecord.getId())).thenReturn(List.of()); // No docs

        KycException exception = assertThrows(KycException.class, () -> kycService.submitForReview(userId));
        assertTrue(exception.getMessage().contains("Mandatory documents"));
    }

    @Test
    void testSubmitForReviewSuccess() {
        when(recordRepository.findByUserId(userId)).thenReturn(Optional.of(kycRecord));
        List<KycDocument> documents = List.of(
                KycDocument.builder().documentType(DocumentType.PAN).build(),
                KycDocument.builder().documentType(DocumentType.AADHAAR).build()
        );
        when(documentRepository.findByKycRecordId(kycRecord.getId())).thenReturn(documents);

        KycResponse response = kycService.submitForReview(userId);
        assertNotNull(response);
        assertEquals(KycStatus.UNDER_REVIEW, response.getStatus());
        verify(auditClient).logEvent(any());
    }

    @Test
    void testApproveKycSuccess() {
        kycRecord.setStatus(KycStatus.UNDER_REVIEW);
        when(recordRepository.findById(kycRecord.getId())).thenReturn(Optional.of(kycRecord));

        KycResponse response = kycService.approveKyc(kycRecord.getId());
        assertNotNull(response);
        assertEquals(KycStatus.APPROVED, response.getStatus());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void testRejectKycSuccess() {
        kycRecord.setStatus(KycStatus.UNDER_REVIEW);
        when(recordRepository.findById(kycRecord.getId())).thenReturn(Optional.of(kycRecord));

        KycResponse response = kycService.rejectKyc(kycRecord.getId(), "Illegible Document");
        assertNotNull(response);
        assertEquals(KycStatus.REJECTED, response.getStatus());
        assertEquals("Illegible Document", response.getRemarks());
        verify(notificationClient).sendNotification(any());
    }

    @Test
    void testRejectKycMissingRemarks() {
        assertThrows(KycException.class, () -> kycService.rejectKyc(kycRecord.getId(), "   "));
    }
}
