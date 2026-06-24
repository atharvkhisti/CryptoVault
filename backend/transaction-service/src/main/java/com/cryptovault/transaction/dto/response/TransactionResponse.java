package com.cryptovault.transaction.dto.response;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a transaction record returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction execution outcome and status details representation")
public class TransactionResponse {

    @Schema(description = "Unique UUID of the transaction record", example = "92bf8a59-122e-407b-a1bc-cd14c2b9a788")
    private UUID transactionId;

    @Schema(description = "Type of transaction action (DEPOSIT, WITHDRAW, TRANSFER)", example = "TRANSFER")
    private TransactionType type;

    @Schema(description = "Status of transaction lifecycle (PENDING, APPROVED, REJECTED, COMPLETED)", example = "COMPLETED")
    private TransactionStatus status;

    @Schema(description = "Amount of funds transacted", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Supported cryptocurrency type", example = "BTC")
    private CurrencyType currency;

    @Schema(description = "Unique bank or platform transaction reference string identifier", example = "TXN-171881881912")
    private String referenceNumber;

    @Schema(description = "Timestamp when the transaction request was initialized", example = "2026-06-19T19:03:49")
    private LocalDateTime timestamp;
}
