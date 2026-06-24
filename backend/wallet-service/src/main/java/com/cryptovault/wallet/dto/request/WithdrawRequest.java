package com.cryptovault.wallet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO containing parameters required to withdraw assets from a wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for performing a withdrawal")
public class WithdrawRequest {

    @NotNull(message = "Wallet identifier is required")
    @Schema(description = "Target wallet UUID identifier", example = "79b33a59-122e-407b-a1bc-cd14c2b9a710", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID walletId;

    @NotNull(message = "Withdrawal amount is required")
    @Positive(message = "Withdrawal amount must be greater than zero")
    @Schema(description = "Withdrawal quantity amount (must be positive)", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
