package com.cryptovault.risk.dto.response;

import com.cryptovault.common.enums.RiskLevel;
import com.cryptovault.common.enums.RiskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>RiskResponse</h3>
 *
 * <p><b>Why it exists:</b> Payload DTO returned on successful risk evaluations or detail query calls.</p>
 * <p><b>Architectural Layer:</b> DTO / Interface Layer.</p>
 * <p><b>Design Patterns Used:</b> Data Transfer Object Pattern, Builder Pattern.</p>
 * <p><b>Banking Relevance:</b> Summarizes risk analysis details for front-office systems or other backend services.</p>
 * <p><b>Scalability Considerations:</b> Serialization-friendly design with slim fields mapping.</p>
 * <p><b>Interview Talking Points:</b> Shields internal database columns and entity mappings from REST network protocols contracts.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Risk assessment evaluation outcome details representation")
public class RiskResponse {

    @Schema(description = "Unique UUID of the risk assessment record", example = "d2bf8a59-122e-407b-a1bc-cd14c2b9a811")
    private UUID id;

    @Schema(description = "UUID of the user evaluated", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Optional transaction UUID evaluated", example = "92bf8a59-122e-407b-a1bc-cd14c2b9a788")
    private UUID transactionId;

    @Schema(description = "Assessed risk classification level (LOW, MEDIUM, HIGH)", example = "LOW")
    private RiskLevel riskLevel;

    @Schema(description = "Overall status action decision (APPROVED, REJECTED, FLAG_MANUAL_REVIEW)", example = "APPROVED")
    private RiskStatus status;

    @Schema(description = "Calculated risk score integer (range 0 to 100)", example = "12")
    private Integer riskScore;

    @Schema(description = "Name of the fraud detection rule triggered, if any", example = "SINGLE_TRANSFER_LIMIT_EXCEEDED")
    private String triggeredRule;

    @Schema(description = "Compliance analyst comments or system reasoning explanation", example = "Transaction amount is within low-risk limits.")
    private String comments;

    @Schema(description = "Timestamp when the risk assessment record was registered", example = "2026-06-19T19:03:49")
    private LocalDateTime createdAt;
}
