package com.cryptovault.wallet.mapper;

import com.cryptovault.wallet.entity.Wallet;
import com.cryptovault.wallet.dto.response.WalletResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper component converting {@link Wallet} entities to {@link WalletResponse} DTOs.
 */
@Component
public class WalletMapper {

    /**
     * Map Wallet entity to WalletResponse DTO.
     *
     * @param wallet the Wallet entity
     * @return the mapped WalletResponse DTO
     */
    public WalletResponse toResponse(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .build();
    }
}
