package com.cryptovault.transaction.controller;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.common.security.JwtUserPrincipal;
import com.cryptovault.transaction.dto.request.DepositRequest;
import com.cryptovault.transaction.dto.request.TransferRequest;
import com.cryptovault.transaction.dto.request.WithdrawRequest;
import com.cryptovault.transaction.dto.response.TransactionResponse;
import com.cryptovault.transaction.security.JwtUserInterceptorFilter;
import com.cryptovault.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST Endpoint unit tests for verifying controller URL routings and input payload mappings
 * using Spring MockMvc.
 */
@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtUserInterceptorFilter jwtUserInterceptorFilter;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();

        // Inject mock authentication into context
        JwtUserPrincipal principal = JwtUserPrincipal.builder()
                .userId(userId)
                .email("test@cryptovault.com")
                .role("USER")
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void transfer_Success() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .senderWalletId(walletId)
                .receiverWalletId(UUID.randomUUID())
                .amount(new BigDecimal("10.0"))
                .description("Transfer DTO")
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("10.0"))
                .currency(CurrencyType.USDT)
                .referenceNumber("TXN-123456")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.transfer(eq(userId), any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.referenceNumber").value("TXN-123456"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void deposit_Success() throws Exception {
        DepositRequest request = new DepositRequest(walletId, new BigDecimal("50.0"), "Deposit DTO");

        TransactionResponse response = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("50.0"))
                .currency(CurrencyType.USDT)
                .referenceNumber("TXN-DEPOSIT")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.deposit(eq(userId), any(DepositRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.referenceNumber").value("TXN-DEPOSIT"));
    }

    @Test
    void withdraw_Success() throws Exception {
        WithdrawRequest request = new WithdrawRequest(walletId, new BigDecimal("20.0"), "Withdraw DTO");

        TransactionResponse response = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("20.0"))
                .currency(CurrencyType.USDT)
                .referenceNumber("TXN-WITHDRAW")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.withdraw(eq(userId), any(WithdrawRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.referenceNumber").value("TXN-WITHDRAW"));
    }

    @Test
    void getTransactionHistory_Success() throws Exception {
        TransactionResponse txn = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("100.0"))
                .currency(CurrencyType.USDT)
                .referenceNumber("TXN-1")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.getTransactionHistory(userId)).thenReturn(List.of(txn));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].referenceNumber").value("TXN-1"));
    }
}
