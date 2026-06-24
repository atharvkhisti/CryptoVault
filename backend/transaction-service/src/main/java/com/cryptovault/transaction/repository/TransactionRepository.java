package com.cryptovault.transaction.repository;

import com.cryptovault.common.enums.TransactionStatus;
import com.cryptovault.common.enums.TransactionType;
import com.cryptovault.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access repository for performing CRUD operations on {@link Transaction} tables.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Retrieve all transactions initiated by a user.
     *
     * @param userId user identifier
     * @return list of transactions
     */
    List<Transaction> findByUserId(UUID userId);

    /**
     * Retrieve all transactions initiated by a user ordered by creation date descending.
     *
     * @param userId user identifier
     * @return list of transactions sorted chronologically (latest first)
     */
    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Retrieve transactions matching a specific status.
     *
     * @param status transaction status
     * @return list of transactions
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Retrieve transactions matching a specific type.
     *
     * @param type transaction type
     * @return list of transactions
     */
    List<Transaction> findByType(TransactionType type);

    /**
     * Retrieve a transaction matching a unique reference number.
     *
     * @param referenceNumber unique transaction reference code
     * @return an Optional containing the transaction, if found
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Retrieve all transactions ordered by creation date descending.
     *
     * @return list of all transactions sorted chronologically
     */
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
