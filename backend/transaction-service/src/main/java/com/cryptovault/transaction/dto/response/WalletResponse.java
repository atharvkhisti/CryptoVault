package com.cryptovault.transaction.dto.response;

import com.cryptovault.common.enums.CurrencyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing the state of a wallet returned by the Wallet Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wallet balance and active currency details representation")
public class WalletResponse {

    @Schema(description = "Unique identifier of the wallet", example = "79b33a59-122e-407b-a1bc-cd14c2b9a710")
    private UUID walletId;

    @Schema(description = "Unique identifier of the user who owns the wallet", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Supported cryptocurrency type", example = "BTC")
    private CurrencyType currency;

    @Schema(description = "Current available balance of the cryptocurrency wallet", example = "2.35000000")
    private BigDecimal balance;
}
