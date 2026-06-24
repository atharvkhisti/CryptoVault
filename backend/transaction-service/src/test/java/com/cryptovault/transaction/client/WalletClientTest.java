package com.cryptovault.transaction.client;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.transaction.dto.response.WalletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for verifying that the Feign Client context compiles, initializes, and maps methods correctly.
 */
@SpringBootTest
class WalletClientTest {

    @MockitoBean
    private WalletClient walletClient;

    /**
     * Verifies getWallet method execution and mapping outcome.
     */
    @Test
    void getWallet_Success() {
        UUID walletId = UUID.randomUUID();
        WalletResponse mockResponse = WalletResponse.builder()
                .walletId(walletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("100.0"))
                .build();

        when(walletClient.getWallet(walletId)).thenReturn(ApiResponse.success("Success", mockResponse));

        ApiResponse<WalletResponse> response = walletClient.getWallet(walletId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(walletId, response.getData().getWalletId());
        assertEquals(new BigDecimal("100.0"), response.getData().getBalance());
        verify(walletClient).getWallet(walletId);
    }

    /**
     * Verifies debitWallet method execution and mapping outcome.
     */
    @Test
    void debitWallet_Success() {
        UUID walletId = UUID.randomUUID();
        WalletResponse mockResponse = WalletResponse.builder()
                .walletId(walletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("90.0"))
                .build();

        when(walletClient.debitWallet(walletId, new BigDecimal("10.0"))).thenReturn(ApiResponse.success("Success", mockResponse));

        ApiResponse<WalletResponse> response = walletClient.debitWallet(walletId, new BigDecimal("10.0"));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(new BigDecimal("90.0"), response.getData().getBalance());
        verify(walletClient).debitWallet(walletId, new BigDecimal("10.0"));
    }

    /**
     * Verifies creditWallet method execution and mapping outcome.
     */
    @Test
    void creditWallet_Success() {
        UUID walletId = UUID.randomUUID();
        WalletResponse mockResponse = WalletResponse.builder()
                .walletId(walletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("110.0"))
                .build();

        when(walletClient.creditWallet(walletId, new BigDecimal("10.0"))).thenReturn(ApiResponse.success("Success", mockResponse));

        ApiResponse<WalletResponse> response = walletClient.creditWallet(walletId, new BigDecimal("10.0"));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(new BigDecimal("110.0"), response.getData().getBalance());
        verify(walletClient).creditWallet(walletId, new BigDecimal("10.0"));
    }
}
