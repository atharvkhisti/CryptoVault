package com.cryptovault.risk.dto.response;

import com.cryptovault.common.enums.CurrencyType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <h3>WalletResponse</h3>
 *
 * <p><b>Why it exists:</b> Maps JSON response payloads representing wallet accounts from the Wallet Service.</p>
 * <p><b>Architectural Layer:</b> DTO / Integration Layer.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID walletId;
    private CurrencyType currency;
    private BigDecimal balance;
}
