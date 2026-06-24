package com.cryptovault.transaction.service;

import com.cryptovault.common.dto.ApiResponse;
import com.cryptovault.common.enums.ErrorCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class orchestrating transaction lifecycle management, balance transfers,
 * deposits, withdrawals, validation, and microservice client coordination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final WalletClient walletClient;

    /**
     * Executes a funds transfer transaction from a sender wallet to a receiver wallet.
     * Implements a local transaction history log and compensating remote calls to recover from partial failures.
     *
     * @param userId  authenticated user's UUID (must own sender wallet)
     * @param request transfer parameters
     * @return details of the completed transaction
     */
    public TransactionResponse transfer(UUID userId, TransferRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        if (request.getSenderWalletId().equals(request.getReceiverWalletId())) {
            throw new InvalidTransactionException("Sender and receiver wallets cannot be the same");
        }

        // Fetch wallet details from Wallet Service
        WalletResponse senderWallet = fetchWalletOrThrow(request.getSenderWalletId());
        WalletResponse receiverWallet = fetchWalletOrThrow(request.getReceiverWalletId());

        // Validate sender wallet ownership
        if (!senderWallet.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to sender wallet", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // Validate currency matching
        if (senderWallet.getCurrency() != receiverWallet.getCurrency()) {
            throw new InvalidTransactionException("Currencies of sender and receiver wallets must match");
        }

        // Validate sufficient balance
        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in sender wallet");
        }

        // Create transaction in PENDING state
        Transaction transaction = Transaction.builder()
                .senderWalletId(request.getSenderWalletId())
                .receiverWalletId(request.getReceiverWalletId())
                .userId(userId)
                .amount(request.getAmount())
                .currency(senderWallet.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .referenceNumber(generateReferenceNumber())
                .description(request.getDescription())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            // Debit the sender wallet
            walletClient.debitWallet(request.getSenderWalletId(), request.getAmount());

            try {
                // Credit the receiver wallet
                walletClient.creditWallet(request.getReceiverWalletId(), request.getAmount());
            } catch (Exception ex) {
                log.error("Failed to credit receiver wallet: {}. Attempting rollback of sender debit.", ex.getMessage());
                // Compensating action: credit back the sender wallet
                try {
                    walletClient.creditWallet(request.getSenderWalletId(), request.getAmount());
                } catch (Exception rollbackEx) {
                    log.error("CRITICAL: Failed to rollback debit for sender wallet {}: {}", request.getSenderWalletId(), rollbackEx.getMessage());
                }
                throw ex;
            }

            // Mark transaction as COMPLETED
            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            Transaction completedTransaction = transactionRepository.save(savedTransaction);
            return transactionMapper.toResponse(completedTransaction);

        } catch (Exception ex) {
            // Mark transaction as FAILED
            savedTransaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            throw new InvalidTransactionException("Transfer failed: " + ex.getMessage());
        }
    }

    /**
     * Executes a deposit transaction to a wallet.
     *
     * @param userId  authenticated user's UUID (must own target wallet)
     * @param request deposit parameters
     * @return details of the completed transaction
     */
    public TransactionResponse deposit(UUID userId, DepositRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero");
        }

        WalletResponse wallet = fetchWalletOrThrow(request.getWalletId());

        if (!wallet.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to target wallet", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Transaction transaction = Transaction.builder()
                .receiverWalletId(request.getWalletId())
                .userId(userId)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .referenceNumber(generateReferenceNumber())
                .description(request.getDescription())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            walletClient.creditWallet(request.getWalletId(), request.getAmount());

            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            Transaction completedTransaction = transactionRepository.save(savedTransaction);
            return transactionMapper.toResponse(completedTransaction);
        } catch (Exception ex) {
            savedTransaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            throw new InvalidTransactionException("Deposit failed: " + ex.getMessage());
        }
    }

    /**
     * Executes a withdrawal transaction from a wallet.
     *
     * @param userId  authenticated user's UUID (must own source wallet)
     * @param request withdrawal parameters
     * @return details of the completed transaction
     */
    public TransactionResponse withdraw(UUID userId, WithdrawRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero");
        }

        WalletResponse wallet = fetchWalletOrThrow(request.getWalletId());

        if (!wallet.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to target wallet", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in target wallet");
        }

        Transaction transaction = Transaction.builder()
                .senderWalletId(request.getWalletId())
                .userId(userId)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.PENDING)
                .referenceNumber(generateReferenceNumber())
                .description(request.getDescription())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            walletClient.debitWallet(request.getWalletId(), request.getAmount());

            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            Transaction completedTransaction = transactionRepository.save(savedTransaction);
            return transactionMapper.toResponse(completedTransaction);
        } catch (Exception ex) {
            savedTransaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            throw new InvalidTransactionException("Withdrawal failed: " + ex.getMessage());
        }
    }

    /**
     * Retrieves all transaction history records for a user.
     *
     * @param userId user identifier
     * @return list of transaction responses
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(UUID userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific transaction detail by its ID.
     *
     * @param userId        authenticated user UUID
     * @param transactionId transaction UUID
     * @return transaction response
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        if (!transaction.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to transaction resource", ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return transactionMapper.toResponse(transaction);
    }

    /**
     * Retrieves transactions belonging to the user filtered by status.
     *
     * @param userId authenticated user UUID
     * @param status status filter
     * @return list of matching transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByStatus(UUID userId, TransactionStatus status) {
        return transactionRepository.findByStatus(status).stream()
                .filter(t -> t.getUserId().equals(userId))
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    private WalletResponse fetchWalletOrThrow(UUID walletId) {
        try {
            ApiResponse<WalletResponse> response = walletClient.getWallet(walletId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException("Wallet not found or invalid response from wallet service", ErrorCode.WALLET_NOT_FOUND);
            }
            return response.getData();
        } catch (Exception ex) {
            throw new BusinessException("Failed to fetch wallet information: " + ex.getMessage(), ErrorCode.WALLET_NOT_FOUND);
        }
    }

    private String generateReferenceNumber() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
