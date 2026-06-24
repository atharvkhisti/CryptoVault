package com.cryptovault.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload class mapping parameters to request a withdrawal transaction from a wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for performing a wallet withdrawal")
public class WithdrawRequest {

    @NotNull(message = "Wallet ID is required")
    @Schema(description = "UUID of the wallet to withdraw funds from", example = "79b33a59-122e-407b-a1bc-cd14c2b9a710", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID walletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Withdrawal amount must be greater than zero")
    @Schema(description = "Withdrawal quantity amount (must be positive)", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Optional transaction description or note", example = "ATM cashout conversion")
    private String description;
}
