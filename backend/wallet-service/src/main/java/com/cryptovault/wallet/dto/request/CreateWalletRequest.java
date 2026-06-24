package com.cryptovault.wallet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.cryptovault.common.enums.CurrencyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing parameters required to create a new wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for creating a new wallet")
public class CreateWalletRequest {

    @NotNull(message = "Currency type is required")
    @Schema(description = "Currency type classification", example = "BTC", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrencyType currency;
}
