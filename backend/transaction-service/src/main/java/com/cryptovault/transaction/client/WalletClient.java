package com.cryptovault.transaction.client;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.transaction.config.FeignClientConfig;
import com.cryptovault.transaction.dto.response.WalletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Feign client defining the contract for communicating with the Wallet Service.
 * Forwarding JWT authorization header via {@link FeignClientConfig}.
 */
@FeignClient(
        name = "wallet-service",
        url = "${application.client.wallet-service.url:http://localhost:8081}",
        configuration = FeignClientConfig.class
)
public interface WalletClient {

    /**
     * Call wallet-service to fetch wallet by ID.
     *
     * @param id wallet UUID
     * @return standard API response envelope containing WalletResponse
     */
    @GetMapping("/api/wallets/{id}")
    ApiResponse<WalletResponse> getWallet(@PathVariable("id") UUID id);

    /**
     * Call wallet-service to debit standard amount from wallet.
     *
     * @param id     wallet UUID
     * @param amount amount to deduct
     * @return updated wallet details
     */
    @PostMapping("/api/wallets/{id}/debit")
    ApiResponse<WalletResponse> debitWallet(
            @PathVariable("id") UUID id,
            @RequestParam("amount") BigDecimal amount
    );

    /**
     * Call wallet-service to credit standard amount to wallet.
     *
     * @param id     wallet UUID
     * @param amount amount to add
     * @return updated wallet details
     */
    @PostMapping("/api/wallets/{id}/credit")
    ApiResponse<WalletResponse> creditWallet(
            @PathVariable("id") UUID id,
            @RequestParam("amount") BigDecimal amount
    );
}
