package com.cryptovault.wallet.service;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.common.enums.ErrorCode;
import com.cryptovault.common.exception.BusinessException;
import com.cryptovault.wallet.dto.request.CreateWalletRequest;
import com.cryptovault.wallet.dto.request.DepositRequest;
import com.cryptovault.wallet.dto.request.WithdrawRequest;
import com.cryptovault.wallet.dto.response.WalletResponse;
import com.cryptovault.wallet.entity.Wallet;
import com.cryptovault.wallet.mapper.WalletMapper;
import com.cryptovault.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests validating core business rules, deposit, withdrawal operations,
 * and currency limits inside {@link WalletService}.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void createWallet_ShouldCreateSuccessfully_WhenWalletDoesNotExist() {
        UUID userId = UUID.randomUUID();
        CreateWalletRequest request = CreateWalletRequest.builder()
                .currency(CurrencyType.BTC)
                .build();

        WalletResponse walletResponse = WalletResponse.builder()
                .walletId(UUID.randomUUID())
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.ZERO)
                .build();

        when(walletRepository.existsByUserIdAndCurrency(userId, CurrencyType.BTC)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletMapper.toResponse(any(Wallet.class))).thenReturn(walletResponse);

        WalletResponse response = walletService.createWallet(userId, request);

        assertNotNull(response);
        assertEquals(CurrencyType.BTC, response.getCurrency());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void createWallet_ShouldThrowException_WhenWalletAlreadyExists() {
        UUID userId = UUID.randomUUID();
        CreateWalletRequest request = CreateWalletRequest.builder()
                .currency(CurrencyType.BTC)
                .build();

        when(walletRepository.existsByUserIdAndCurrency(userId, CurrencyType.BTC)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                walletService.createWallet(userId, request)
        );

        assertEquals(ErrorCode.INVALID_TRANSACTION, exception.getErrorCode());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void deposit_ShouldUpdateBalanceSuccessfully_WhenParametersAreValid() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        DepositRequest request = DepositRequest.builder()
                .walletId(walletId)
                .amount(BigDecimal.valueOf(1.5))
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.ZERO)
                .build();

        WalletResponse expectedResponse = WalletResponse.builder()
                .walletId(walletId)
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.valueOf(1.5))
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletMapper.toResponse(any(Wallet.class))).thenReturn(expectedResponse);

        WalletResponse response = walletService.deposit(userId, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(1.5), response.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void deposit_ShouldThrowException_WhenWalletNotFound() {
        UUID userId = UUID.randomUUID();
        DepositRequest request = DepositRequest.builder()
                .walletId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .build();

        when(walletRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                walletService.deposit(userId, request)
        );

        assertEquals(ErrorCode.WALLET_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deposit_ShouldThrowException_WhenUserDoesNotOwnWallet() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        DepositRequest request = DepositRequest.builder()
                .walletId(walletId)
                .amount(BigDecimal.TEN)
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .userId(otherUserId)
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                walletService.deposit(userId, request)
        );

        assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());
    }

    @Test
    void deposit_ShouldThrowException_WhenAmountIsZeroOrNegative() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        DepositRequest request = DepositRequest.builder()
                .walletId(walletId)
                .amount(BigDecimal.valueOf(-5))
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                walletService.deposit(userId, request)
        );

        assertEquals(ErrorCode.INVALID_TRANSACTION, exception.getErrorCode());
    }

    @Test
    void withdraw_ShouldUpdateBalanceSuccessfully_WhenFundsAreSufficient() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        WithdrawRequest request = WithdrawRequest.builder()
                .walletId(walletId)
                .amount(BigDecimal.valueOf(0.5))
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.ONE)
                .build();

        WalletResponse expectedResponse = WalletResponse.builder()
                .walletId(walletId)
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.valueOf(0.5))
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletMapper.toResponse(any(Wallet.class))).thenReturn(expectedResponse);

        WalletResponse response = walletService.withdraw(userId, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(0.5), response.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void withdraw_ShouldThrowException_WhenFundsAreInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        WithdrawRequest request = WithdrawRequest.builder()
                .walletId(walletId)
                .amount(BigDecimal.TEN)
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .currency(CurrencyType.BTC)
                .balance(BigDecimal.ONE)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                walletService.withdraw(userId, request)
        );

        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getUserWallets_ShouldReturnMappedList() {
        UUID userId = UUID.randomUUID();
        Wallet wallet1 = Wallet.builder().id(UUID.randomUUID()).userId(userId).currency(CurrencyType.BTC).balance(BigDecimal.ZERO).build();
        Wallet wallet2 = Wallet.builder().id(UUID.randomUUID()).userId(userId).currency(CurrencyType.ETH).balance(BigDecimal.ONE).build();

        when(walletRepository.findByUserId(userId)).thenReturn(List.of(wallet1, wallet2));
        when(walletMapper.toResponse(wallet1)).thenReturn(WalletResponse.builder().currency(CurrencyType.BTC).balance(BigDecimal.ZERO).build());
        when(walletMapper.toResponse(wallet2)).thenReturn(WalletResponse.builder().currency(CurrencyType.ETH).balance(BigDecimal.ONE).build());

        List<WalletResponse> response = walletService.getUserWallets(userId);

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(CurrencyType.BTC, response.get(0).getCurrency());
        assertEquals(CurrencyType.ETH, response.get(1).getCurrency());
    }
}
