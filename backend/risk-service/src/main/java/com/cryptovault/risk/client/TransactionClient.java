package com.cryptovault.risk.client;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.risk.config.FeignClientConfig;
import com.cryptovault.risk.dto.response.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * <h3>TransactionClient</h3>
 *
 * <p><b>Why it exists:</b> Feign client defining REST integrations to pull transaction historical records.</p>
 * <p><b>Architectural Layer:</b> Integration / Client Layer.</p>
 * <p><b>Design Patterns Used:</b> Proxy Pattern (Feign abstraction).</p>
 */
@FeignClient(
        name = "transaction-service",
        url = "${application.client.transaction-service.url:http://localhost:8082}",
        configuration = FeignClientConfig.class
)
public interface TransactionClient {

    /**
     * Fetch user transaction history records downstream.
     *
     * @return standard response envelope wrapping list of transactions
     */
    @GetMapping("/api/transactions")
    ApiResponse<List<TransactionResponse>> getTransactionHistory();
}
