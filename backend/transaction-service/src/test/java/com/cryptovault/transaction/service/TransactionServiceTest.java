package com.cryptovault.transaction.service;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.common.exception.BusinessException;
import com.cryptovault.transaction.client.WalletClient;
import com.cryptovault.transaction.dto.request.DepositRequest;
import com.cryptovault.transaction.dto.request.TransferRequest;
import com.cryptovault.transaction.dto.request.WithdrawRequest;
import com.cryptovault.transaction.dto.response.TransactionResponse;
import com.cryptovault.transaction.dto.response.WalletResponse;
import com.cryptovault.transaction.entity.Transaction;
import com.cryptovault.transaction.exception.InsufficientBalanceException;
import com.cryptovault.transaction.exception.InvalidTransactionException;
import com.cryptovault.transaction.exception.TransactionNotFoundException;
import com.cryptovault.transaction.mapper.TransactionMapper;
import com.cryptovault.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying business operations and failure recovery logic inside {@link TransactionService}.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletClient walletClient;

    @Spy
    private TransactionMapper transactionMapper = new TransactionMapper();

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID senderWalletId;
    private UUID receiverWalletId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();
    }

    @Test
    void transfer_Success() {
        TransferRequest request = TransferRequest.builder()
                .senderWalletId(senderWalletId)
                .receiverWalletId(receiverWalletId)
                .amount(new BigDecimal("100.0"))
                .description("Test Transfer")
                .build();

        WalletResponse senderWallet = WalletResponse.builder()
                .walletId(senderWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("500.0"))
                .build();
        senderWallet.setUserId(userId);

        WalletResponse receiverWallet = WalletResponse.builder()
                .walletId(receiverWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("50.0"))
                .build();
        receiverWallet.setUserId(UUID.randomUUID());

        when(walletClient.getWallet(senderWalletId)).thenReturn(ApiResponse.success("Success", senderWallet));
        when(walletClient.getWallet(receiverWalletId)).thenReturn(ApiResponse.success("Success", receiverWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.transfer(userId, request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        assertEquals(TransactionType.TRANSFER, response.getType());
        assertEquals(new BigDecimal("100.0"), response.getAmount());
        verify(walletClient).debitWallet(senderWalletId, new BigDecimal("100.0"));
        verify(walletClient).creditWallet(receiverWalletId, new BigDecimal("100.0"));
    }

    @Test
    void transfer_SelfTransfer_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .senderWalletId(senderWalletId)
                .receiverWalletId(senderWalletId) // Self transfer
                .amount(new BigDecimal("100.0"))
                .build();

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(userId, request));
    }

    @Test
    void transfer_InsufficientBalance_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .senderWalletId(senderWalletId)
                .receiverWalletId(receiverWalletId)
                .amount(new BigDecimal("100.0"))
                .build();

        WalletResponse senderWallet = WalletResponse.builder()
                .walletId(senderWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("50.0")) // Insufficient balance
                .build();
        senderWallet.setUserId(userId);

        WalletResponse receiverWallet = WalletResponse.builder()
                .walletId(receiverWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("50.0"))
                .build();
        receiverWallet.setUserId(UUID.randomUUID());

        when(walletClient.getWallet(senderWalletId)).thenReturn(ApiResponse.success("Success", senderWallet));
        when(walletClient.getWallet(receiverWalletId)).thenReturn(ApiResponse.success("Success", receiverWallet));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transfer(userId, request));
    }

    @Test
    void transfer_CurrencyMismatch_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .senderWalletId(senderWalletId)
                .receiverWalletId(receiverWalletId)
                .amount(new BigDecimal("100.0"))
                .build();

        WalletResponse senderWallet = WalletResponse.builder()
                .walletId(senderWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("500.0"))
                .build();
        senderWallet.setUserId(userId);

        WalletResponse receiverWallet = WalletResponse.builder()
                .walletId(receiverWalletId)
                .currency(CurrencyType.BTC) // Currency mismatch
                .balance(new BigDecimal("0.5"))
                .build();
        receiverWallet.setUserId(UUID.randomUUID());

        when(walletClient.getWallet(senderWalletId)).thenReturn(ApiResponse.success("Success", senderWallet));
        when(walletClient.getWallet(receiverWalletId)).thenReturn(ApiResponse.success("Success", receiverWallet));

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(userId, request));
    }

    @Test
    void transfer_CompensatingActionOnFailure() {
        TransferRequest request = TransferRequest.builder()
                .senderWalletId(senderWalletId)
                .receiverWalletId(receiverWalletId)
                .amount(new BigDecimal("100.0"))
                .build();

        WalletResponse senderWallet = WalletResponse.builder()
                .walletId(senderWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("500.0"))
                .build();
        senderWallet.setUserId(userId);

        WalletResponse receiverWallet = WalletResponse.builder()
                .walletId(receiverWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("50.0"))
                .build();
        receiverWallet.setUserId(UUID.randomUUID());

        when(walletClient.getWallet(senderWalletId)).thenReturn(ApiResponse.success("Success", senderWallet));
        when(walletClient.getWallet(receiverWalletId)).thenReturn(ApiResponse.success("Success", receiverWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock credit to throw exception to test compensation
        when(walletClient.debitWallet(senderWalletId, new BigDecimal("100.0"))).thenReturn(null);
        when(walletClient.creditWallet(receiverWalletId, new BigDecimal("100.0"))).thenThrow(new RuntimeException("Network down"));

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(userId, request));

        // Verify that compensating credit on sender was called to refund the funds
        verify(walletClient).creditWallet(senderWalletId, new BigDecimal("100.0"));
    }

    @Test
    void deposit_Success() {
        DepositRequest request = new DepositRequest(senderWalletId, new BigDecimal("250.0"), "Test Deposit");
        WalletResponse wallet = WalletResponse.builder()
                .walletId(senderWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("10.0"))
                .build();
        wallet.setUserId(userId);

        when(walletClient.getWallet(senderWalletId)).thenReturn(ApiResponse.success("Success", wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.deposit(userId, request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        assertEquals(TransactionType.DEPOSIT, response.getType());
        assertEquals(new BigDecimal("250.0"), response.getAmount());
        verify(walletClient).creditWallet(senderWalletId, new BigDecimal("250.0"));
    }

    @Test
    void withdraw_Success() {
        WithdrawRequest request = new WithdrawRequest(senderWalletId, new BigDecimal("50.0"), "Test Withdraw");
        WalletResponse wallet = WalletResponse.builder()
                .walletId(senderWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("100.0"))
                .build();
        wallet.setUserId(userId);

        when(walletClient.getWallet(senderWalletId)).thenReturn(ApiResponse.success("Success", wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.withdraw(userId, request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        assertEquals(TransactionType.WITHDRAW, response.getType());
        assertEquals(new BigDecimal("50.0"), response.getAmount());
        verify(walletClient).debitWallet(senderWalletId, new BigDecimal("50.0"));
    }

    @Test
    void withdraw_InsufficientBalance_ThrowsException() {
        WithdrawRequest request = new WithdrawRequest(senderWalletId, new BigDecimal("150.0"), "Test Withdraw Overdraft");
        WalletResponse wallet = WalletResponse.builder()
                .walletId(senderWalletId)
                .currency(CurrencyType.USDT)
                .balance(new BigDecimal("100.0"))
                .build();
        wallet.setUserId(userId);

        when(walletClient.getWallet(senderWalletId)).thenReturn(ApiResponse.success("Success", wallet));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(userId, request));
    }

    @Test
    void getTransactionById_Success() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .userId(userId)
                .amount(new BigDecimal("50.0"))
                .currency(CurrencyType.USDT)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .referenceNumber("REF123")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getTransactionById(userId, transactionId);

        assertNotNull(response);
        assertEquals(transactionId, response.getTransactionId());
        assertEquals("REF123", response.getReferenceNumber());
    }

    @Test
    void getTransactionById_AccessDenied_ThrowsException() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .userId(UUID.randomUUID()) // Different user
                .amount(new BigDecimal("50.0"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(BusinessException.class, () -> transactionService.getTransactionById(userId, transactionId));
    }
}
