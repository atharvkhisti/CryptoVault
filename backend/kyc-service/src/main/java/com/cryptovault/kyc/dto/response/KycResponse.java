package com.cryptovault.kyc.dto.response;

import com.cryptovault.common.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <h3>KycResponse</h3>
 *
 * <p><b>Why it exists:</b> DTO response representing a detailed KYC status snapshot payload.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC compliance request registry and verification status details representation")
public class KycResponse {

    @Schema(description = "Unique UUID of the KYC record", example = "e2bf8a59-122e-407b-a1bc-cd14c2b9a822")
    private UUID kycId;

    @Schema(description = "UUID of the user holding this KYC profile context", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Current lifecycle verification status of KYC", example = "UNDER_REVIEW")
    private KycStatus status;

    @Schema(description = "Review remarks or rejection reasons comments from system or analyst", example = "Document photo is blurry.")
    private String remarks;

    @Schema(description = "Timestamp when the KYC documents were submitted", example = "2026-06-19T19:03:49")
    private LocalDateTime submittedAt;

    @Schema(description = "Timestamp when the KYC review was completed", example = "2026-06-19T19:03:49")
    private LocalDateTime reviewedAt;

    @Schema(description = "Timestamp when the KYC request was originally created", example = "2026-06-19T19:03:49")
    private LocalDateTime createdAt;

    @Schema(description = "List of associated upload documentation details")
    private List<DocumentResponse> documents;
}
