package com.cryptovault.kyc.client;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.kyc.config.FeignClientConfig;
import com.cryptovault.kyc.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * <h3>AuthClient</h3>
 *
 * <p><b>Why it exists:</b> Feign client defining REST integrations to query user details from the Auth microservice.</p>
 * <p><b>Architectural Layer:</b> Integration / Feign Client Layer.</p>
 * <p><b>Design Patterns Used:</b> Proxy Pattern / Declarative REST Client.</p>
 * <p><b>Financial Compliance Relevance:</b> Verifies user existence and checks user registration status prior to initiating a KYC record setup.</p>
 */
@FeignClient(
        name = "auth-service",
        url = "${application.client.auth-service.url:http://localhost:8083}",
        configuration = FeignClientConfig.class
)
public interface AuthClient {

    /**
     * Retrieve user profile details by UUID from the auth-service database.
     *
     * @param id target user ID
     * @return standard generic response envelope wrapping user response DTO
     */
    @GetMapping("/api/auth/{id}")
    ApiResponse<UserResponse> getUserById(@PathVariable("id") UUID id);
}
