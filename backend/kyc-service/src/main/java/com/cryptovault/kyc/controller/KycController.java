package com.cryptovault.kyc.controller;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.DocumentType;
import com.cryptovault.common.security.JwtUserPrincipal;
import com.cryptovault.kyc.dto.response.KycResponse;
import com.cryptovault.kyc.service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <h3>KycController</h3>
 *
 * <p><b>Why it exists:</b> REST controller exposing API endpoints to perform document uploads, KYC status queries, and reviews approvals/rejections.</p>
 * <p><b>Architectural Layer:</b> Web REST Controller Layer.</p>
 * <p><b>Design Patterns Used:</b> Front Controller Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Direct gate permitting document uploads and submission events, restricting reviews routes to security-authorized roles.</p>
 * <p><b>Scalability Considerations:</b> Employs standard HTTP file multipart mappings, streaming data context directly to the strategy storage layers.</p>
 * <p><b>Interview Talking Points:</b> 
 * 1. Wraps all return payloads inside generic {@link ApiResponse} wrappers.
 * 2. Extracts caller context headers in-memory via Spring Security, mapping user identity automatically.</p>
 */
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "KYC Interface", description = "Endpoints for initiating KYC requests, uploading identity documents, reviewing, and verifying users")
public class KycController {

    private final KycService kycService;

    /**
     * Initializes a KYC record for the currently authenticated user in PENDING state.
     *
     * @return 201 Created status with KycResponse DTO
     */
    @PostMapping
    @Operation(summary = "Initialize KYC Request", description = "Instantiates a new KYC registration process with PENDING status for the authenticated user context.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "KYC record initialized successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation parameters failure", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "KYC request already exists for this user", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<KycResponse>> createKyc() {
        UUID userId = getCurrentUserId();
        log.info("REST call to initiate KYC record for user={}", userId);
        KycResponse response = kycService.createKycRequest(userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("KYC record initialized successfully", response));
    }

    /**
     * Uploads an identity document (PAN, AADHAAR, PASSPORT, DRIVING_LICENSE).
     *
     * @param file uploaded binary file
     * @param documentType identity document type classification
     * @return updated KycResponse DTO
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload KYC Document", description = "Uploads a scan or photograph image file representing user identification (PAN, Aadhaar, Passport, etc.).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Document uploaded successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File size exceeded, unsupported file type or invalid document parameters", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC request record not found for this user", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<KycResponse>> uploadDocument(
            @RequestParam("file") @Parameter(description = "Multipart binary file representation of identity document", required = true) MultipartFile file,
            @RequestParam("documentType") @Parameter(description = "Type classification enum of the uploaded identity doc", example = "PASSPORT", required = true) DocumentType documentType
    ) {
        UUID userId = getCurrentUserId();
        log.info("REST call to upload document type={} for user={}", documentType, userId);
        KycResponse response = kycService.uploadDocument(userId, file, documentType);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }

    /**
     * Submits uploaded files for review, transitioning status to UNDER_REVIEW.
     *
     * @return KycResponse DTO
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit KYC for Review", description = "Locks uploaded identity documents and triggers review tasks by transitioning status from PENDING to UNDER_REVIEW.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC submitted for review successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No documents uploaded or invalid KYC state", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC request record not found for this user", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<KycResponse>> submitKyc() {
        UUID userId = getCurrentUserId();
        log.info("REST call to submit KYC documents for user={}", userId);
        KycResponse response = kycService.submitForReview(userId);
        return ResponseEntity.ok(ApiResponse.success("KYC submitted for review successfully", response));
    }

    /**
     * Approves a KYC verification request (Admin only route).
     *
     * @param kycId record UUID
     * @return updated KycResponse DTO
     */
    @PostMapping("/approve")
    @Operation(summary = "Approve KYC Verification", description = "Approves a submitted KYC request context and transitions status to VERIFIED (Compliance admin access required).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC approved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege credentials required", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC record UUID not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<KycResponse>> approveKyc(
            @RequestParam("kycId") @Parameter(description = "Unique UUID of the target KYC request record", example = "e2bf8a59-122e-407b-a1bc-cd14c2b9a822", required = true) UUID kycId
    ) {
        log.info("REST call to approve KYC record ID={}", kycId);
        KycResponse response = kycService.approveKyc(kycId);
        return ResponseEntity.ok(ApiResponse.success("KYC approved successfully", response));
    }

    /**
     * Rejects a KYC verification request (Admin only route).
     *
     * @param kycId record UUID
     * @param remarks rejection reason details
     * @return updated KycResponse DTO
     */
    @PostMapping("/reject")
    @Operation(summary = "Reject KYC Verification", description = "Rejects a submitted KYC request context and transitions status to REJECTED with specified comments/remarks (Compliance admin access required).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC rejected successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege credentials required", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC record UUID not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<KycResponse>> rejectKyc(
            @RequestParam("kycId") @Parameter(description = "Unique UUID of the target KYC request record", example = "e2bf8a59-122e-407b-a1bc-cd14c2b9a822", required = true) UUID kycId,
            @RequestParam("remarks") @Parameter(description = "Detailed comments or reason describing the rejection outcome", example = "Document copy is expired.", required = true) String remarks
    ) {
        log.info("REST call to reject KYC record ID={} remarks={}", kycId, remarks);
        KycResponse response = kycService.rejectKyc(kycId, remarks);
        return ResponseEntity.ok(ApiResponse.success("KYC rejected successfully", response));
    }

    /**
     * Retrieves the KYC status of a user.
     *
     * @param userId user identifier
     * @return KycResponse DTO
     */
    @GetMapping("/status/{userId}")
    @Operation(summary = "Get User KYC Status", description = "Retrieves the verification details and status properties of user compliance records by user UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC status retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC status record not found for user", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<KycResponse>> getKycStatus(
            @PathVariable("userId") @Parameter(description = "Unique UUID of the target user", example = "123e4567-e89b-12d3-a456-426614174000") UUID userId
    ) {
        log.info("REST call to query KYC status for user={}", userId);
        KycResponse response = kycService.getKycStatus(userId);
        return ResponseEntity.ok(ApiResponse.success("KYC status retrieved successfully", response));
    }

    /**
     * Retrieves details of a specific KYC record by UUID.
     *
     * @param id KYC record ID
     * @return KycResponse DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get KYC Record Details", description = "Retrieves details of a specific KYC compliance request record by its unique UUID ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC record details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC record UUID not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<KycResponse>> getKycById(
            @PathVariable("id") @Parameter(description = "Unique UUID of the KYC request record", example = "e2bf8a59-122e-407b-a1bc-cd14c2b9a822") UUID id
    ) {
        log.info("REST call to query KYC record details for ID={}", id);
        KycResponse response = kycService.getUserKyc(id);
        return ResponseEntity.ok(ApiResponse.success("KYC record details retrieved successfully", response));
    }

    private UUID getCurrentUserId() {
        JwtUserPrincipal principal = (JwtUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getUserId();
    }
}
