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
 * Request payload class mapping the parameters to request a balance transfer between two wallets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for performing a funds transfer between wallets")
public class TransferRequest {

    @NotNull(message = "Sender wallet ID is required")
    @Schema(description = "UUID of the source sender wallet", example = "79b33a59-122e-407b-a1bc-cd14c2b9a710", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID senderWalletId;

    @NotNull(message = "Receiver wallet ID is required")
    @Schema(description = "UUID of the destination receiver wallet", example = "a2b66a59-344e-407b-a2bc-cd14c2b9a722", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID receiverWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Transfer amount must be greater than zero")
    @Schema(description = "Transfer quantity amount (must be positive)", example = "25.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Optional transaction description or note", example = "Payment for services")
    private String description;
}
