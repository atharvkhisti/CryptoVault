package com.cryptovault.kyc.controller;

import com.cryptovault.common.enums.DocumentType;
import com.cryptovault.common.enums.KycStatus;
import com.cryptovault.common.security.JwtUserPrincipal;
import com.cryptovault.kyc.dto.response.KycResponse;
import com.cryptovault.kyc.service.KycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer tests verifying endpoint mappings and response payloads of KycController.
 */
@WebMvcTest(controllers = KycController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class KycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KycService kycService;

    private UUID userId;
    private UUID kycId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        kycId = UUID.randomUUID();
        JwtUserPrincipal principal = JwtUserPrincipal.builder()
                .userId(userId)
                .email("test@cryptovault.com")
                .role("USER")
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void testCreateKycSuccess() throws Exception {
        KycResponse mockResponse = KycResponse.builder()
                .kycId(kycId)
                .userId(userId)
                .status(KycStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .documents(List.of())
                .build();

        when(kycService.createKycRequest(userId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/kyc"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("KYC record initialized successfully"))
                .andExpect(jsonPath("$.data.kycId").value(kycId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(kycService, times(1)).createKycRequest(userId);
    }

    @Test
    @WithMockUser
    void testUploadDocumentSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pan.jpg", MediaType.IMAGE_JPEG_VALUE, "document_bytes".getBytes()
        );

        KycResponse mockResponse = KycResponse.builder()
                .kycId(kycId)
                .userId(userId)
                .status(KycStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(kycService.uploadDocument(eq(userId), any(), eq(DocumentType.PAN))).thenReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/kyc/upload")
                        .file(file)
                        .param("documentType", "PAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"));

        verify(kycService, times(1)).uploadDocument(eq(userId), any(), eq(DocumentType.PAN));
    }

    @Test
    @WithMockUser
    void testSubmitKycSuccess() throws Exception {
        KycResponse mockResponse = KycResponse.builder()
                .kycId(kycId)
                .userId(userId)
                .status(KycStatus.UNDER_REVIEW)
                .submittedAt(LocalDateTime.now())
                .build();

        when(kycService.submitForReview(userId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/kyc/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        verify(kycService, times(1)).submitForReview(userId);
    }

    @Test
    @WithMockUser
    void testApproveKycSuccess() throws Exception {
        KycResponse mockResponse = KycResponse.builder()
                .kycId(kycId)
                .userId(userId)
                .status(KycStatus.APPROVED)
                .reviewedAt(LocalDateTime.now())
                .build();

        when(kycService.approveKyc(kycId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/kyc/approve")
                        .param("kycId", kycId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(kycService, times(1)).approveKyc(kycId);
    }

    @Test
    @WithMockUser
    void testRejectKycSuccess() throws Exception {
        KycResponse mockResponse = KycResponse.builder()
                .kycId(kycId)
                .userId(userId)
                .status(KycStatus.REJECTED)
                .remarks("Invalid address proof doc")
                .reviewedAt(LocalDateTime.now())
                .build();

        when(kycService.rejectKyc(kycId, "Invalid address proof doc")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/kyc/reject")
                        .param("kycId", kycId.toString())
                        .param("remarks", "Invalid address proof doc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.remarks").value("Invalid address proof doc"));

        verify(kycService, times(1)).rejectKyc(kycId, "Invalid address proof doc");
    }

    @Test
    @WithMockUser
    void testGetKycStatusSuccess() throws Exception {
        KycResponse mockResponse = KycResponse.builder()
                .kycId(kycId)
                .userId(userId)
                .status(KycStatus.PENDING)
                .build();

        when(kycService.getKycStatus(userId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/kyc/status/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()));

        verify(kycService, times(1)).getKycStatus(userId);
    }

    @Test
    @WithMockUser
    void testGetKycByIdSuccess() throws Exception {
        KycResponse mockResponse = KycResponse.builder()
                .kycId(kycId)
                .userId(userId)
                .status(KycStatus.APPROVED)
                .build();

        when(kycService.getUserKyc(kycId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/kyc/{id}", kycId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kycId").value(kycId.toString()));

        verify(kycService, times(1)).getUserKyc(kycId);
    }
}
