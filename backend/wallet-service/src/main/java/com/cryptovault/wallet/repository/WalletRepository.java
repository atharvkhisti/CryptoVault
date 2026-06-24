package com.cryptovault.wallet.repository;

import com.cryptovault.common.enums.CurrencyType;
import com.cryptovault.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access repository for performing CRUD operations on {@link Wallet} tables.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Retrieve all wallets owned by a user.
     *
     * @param userId the user's unique identifier
     * @return list of wallets
     */
    List<Wallet> findByUserId(UUID userId);

    /**
     * Retrieve a specific wallet matching a user ID and currency type.
     *
     * @param userId   the user's unique identifier
     * @param currency currency asset type
     * @return an Optional containing the Wallet, if found
     */
    Optional<Wallet> findByUserIdAndCurrency(UUID userId, CurrencyType currency);

    /**
     * Check if a wallet already exists for a user and currency combination.
     *
     * @param userId   the user's unique identifier
     * @param currency currency asset type
     * @return true if the wallet exists, false otherwise
     */
    boolean existsByUserIdAndCurrency(UUID userId, CurrencyType currency);
}
