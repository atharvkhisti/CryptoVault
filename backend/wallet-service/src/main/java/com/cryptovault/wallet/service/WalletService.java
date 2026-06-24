package com.cryptovault.wallet.service;

import com.cryptovault.common.enums.ErrorCode;
import com.cryptovault.common.exception.BusinessException;
import com.cryptovault.wallet.dto.request.CreateWalletRequest;
import com.cryptovault.wallet.dto.request.DepositRequest;
import com.cryptovault.wallet.dto.request.WithdrawRequest;
import com.cryptovault.wallet.dto.response.WalletResponse;
import com.cryptovault.wallet.entity.Wallet;
import com.cryptovault.wallet.mapper.WalletMapper;
import com.cryptovault.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class containing business flow implementations for wallet accounts.
 * Enforces transaction safety rules, asset balances, and transactional integrity.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    /**
     * Initializes a new wallet account for the specified user and currency.
     * Enforces unique constraint of one wallet per currency per user.
     *
     * @param userId  authenticated user's UUID
     * @param request wallet creation parameters
     * @return the created wallet response
     */
    @Transactional
    @SuppressWarnings("unused")
    public WalletResponse createWallet(UUID userId, CreateWalletRequest request) {
        if (walletRepository.existsByUserIdAndCurrency(userId, request.getCurrency())) {
            throw new BusinessException(
                    "Wallet with currency " + request.getCurrency() + " already exists for this user",
                    ErrorCode.INVALID_TRANSACTION
            );
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency(request.getCurrency())
                .balance(BigDecimal.ZERO)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        return walletMapper.toResponse(savedWallet);
    }

    /**
     * Deposits a positive amount of funds into a user's wallet.
     *
     * @param userId  authenticated user's UUID
     * @param request deposit request details
     * @return the updated wallet response
     */
    @Transactional
    @SuppressWarnings("unused")
    public WalletResponse deposit(UUID userId, DepositRequest request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new BusinessException("Wallet not found", ErrorCode.WALLET_NOT_FOUND));

        if (!wallet.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to requested wallet", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Deposit amount must be positive", ErrorCode.INVALID_TRANSACTION);
        }

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        Wallet updatedWallet = walletRepository.save(wallet);
        return walletMapper.toResponse(updatedWallet);
    }

    /**
     * Withdraws funds from a user's wallet, checking for sufficient balance.
     *
     * @param userId  authenticated user's UUID
     * @param request withdrawal request details
     * @return the updated wallet response
     */
    @Transactional
    @SuppressWarnings("unused")
    public WalletResponse withdraw(UUID userId, WithdrawRequest request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new BusinessException("Wallet not found", ErrorCode.WALLET_NOT_FOUND));

        if (!wallet.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to requested wallet", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Withdrawal amount must be positive", ErrorCode.INVALID_TRANSACTION);
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient balance in wallet", ErrorCode.INSUFFICIENT_BALANCE);
        }

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        Wallet updatedWallet = walletRepository.save(wallet);
        return walletMapper.toResponse(updatedWallet);
    }

    /**
     * Retrieves all wallet balances registered to the authenticated user.
     *
     * @param userId authenticated user's UUID
     * @return list of wallet profile responses
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unused")
    public List<WalletResponse> getUserWallets(UUID userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Internal endpoint method to debit a wallet's balance without ownership verification.
     *
     * @param walletId wallet identifier
     * @param amount   amount to subtract
     * @return updated wallet response details
     */
    @Transactional
    public WalletResponse debit(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException("Wallet not found", ErrorCode.WALLET_NOT_FOUND));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Debit amount must be positive", ErrorCode.INVALID_TRANSACTION);
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance in wallet", ErrorCode.INSUFFICIENT_BALANCE);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        Wallet updatedWallet = walletRepository.save(wallet);
        return walletMapper.toResponse(updatedWallet);
    }

    /**
     * Internal endpoint method to credit a wallet's balance without ownership verification.
     *
     * @param walletId wallet identifier
     * @param amount   amount to add
     * @return updated wallet response details
     */
    @Transactional
    public WalletResponse credit(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException("Wallet not found", ErrorCode.WALLET_NOT_FOUND));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Credit amount must be positive", ErrorCode.INVALID_TRANSACTION);
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet updatedWallet = walletRepository.save(wallet);
        return walletMapper.toResponse(updatedWallet);
    }

    /**
     * Internal endpoint method to retrieve wallet info by ID without ownership verification.
     *
     * @param walletId wallet identifier
     * @return wallet details
     */
    @Transactional(readOnly = true)
    public WalletResponse getWalletById(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException("Wallet not found", ErrorCode.WALLET_NOT_FOUND));
        return walletMapper.toResponse(wallet);
    }
}
