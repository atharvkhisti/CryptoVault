package com.cryptovault.risk.dto.request;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <h3>EvaluateRiskRequest</h3>
 *
 * <p><b>Why it exists:</b> Payload DTO received to request a transaction or user risk profile evaluation.</p>
 * <p><b>Architectural Layer:</b> DTO / Interface Layer.</p>
 * <p><b>Design Patterns Used:</b> Data Transfer Object Pattern, Builder Pattern.</p>
 * <p><b>Banking Relevance:</b> Groups the parameters necessary to run compliance rules evaluations before completing money transfers.</p>
 * <p><b>Scalability Considerations:</b> Lightweight, serializable payload context.</p>
 * <p><b>Interview Talking Points:</b> Uses standard bean validation constraints (<code>@NotNull</code>) to block invalid requests at the controller gate.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for performing transaction risk evaluation")
public class EvaluateRiskRequest {

    @NotNull(message = "User ID must not be null")
    @Schema(description = "UUID of the user initiating the transaction", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID userId;

    @Schema(description = "Optional transaction UUID associated with the evaluation", example = "92bf8a59-122e-407b-a1bc-cd14c2b9a788")
    private UUID transactionId;

    @NotNull(message = "Amount must not be null")
    @Schema(description = "Transaction quantity amount to evaluate", example = "15000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotNull(message = "Currency type must not be null")
    @Schema(description = "Supported cryptocurrency type", example = "BTC", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrencyType currency;

    @NotNull(message = "Transaction type must not be null")
    @Schema(description = "Type of transaction action (DEPOSIT, WITHDRAW, TRANSFER)", example = "TRANSFER", requiredMode = Schema.RequiredMode.REQUIRED)
    private TransactionType type;
}
