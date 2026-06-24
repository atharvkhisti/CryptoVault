package com.cryptovault.risk.client;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.risk.config.FeignClientConfig;
import com.cryptovault.risk.dto.response.WalletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * <h3>WalletClient</h3>
 *
 * <p><b>Why it exists:</b> Feign client defining REST integrations to pull wallet balances data.</p>
 * <p><b>Architectural Layer:</b> Integration / Client Layer.</p>
 * <p><b>Design Patterns Used:</b> Proxy Pattern (Feign abstraction).</p>
 */
@FeignClient(
        name = "wallet-service",
        url = "${application.client.wallet-service.url:http://localhost:8081}",
        configuration = FeignClientConfig.class
)
public interface WalletClient {

    /**
     * Fetch user wallets details downstream.
     *
     * @return standard response envelope wrapping list of wallets
     */
    @GetMapping("/api/wallets")
    ApiResponse<List<WalletResponse>> getUserWallets();
}
