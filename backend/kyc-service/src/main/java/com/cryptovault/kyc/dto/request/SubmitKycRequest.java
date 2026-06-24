package com.cryptovault.kyc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * <h3>SubmitKycRequest</h3>
 *
 * <p><b>Why it exists:</b> DTO payload to trigger a KYC record submission for manual compliance review.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitKycRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;
}
