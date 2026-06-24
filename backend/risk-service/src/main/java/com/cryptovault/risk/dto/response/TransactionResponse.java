package com.cryptovault.risk.dto.response;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h3>TransactionResponse</h3>
 *
 * <p><b>Why it exists:</b> Maps JSON response payloads representing transactions from the Transaction Service.</p>
 * <p><b>Architectural Layer:</b> DTO / Integration Layer.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID transactionId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private CurrencyType currency;
    private String referenceNumber;
    private LocalDateTime timestamp;
}
