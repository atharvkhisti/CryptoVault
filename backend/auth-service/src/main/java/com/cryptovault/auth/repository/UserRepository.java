package com.cryptovault.auth.repository;

import com.cryptovault.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository layer interface for performing database queries and operations on {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Query user by their email address.
     *
     * @param email the email address
     * @return an Optional user wrapper
     */
    Optional<User> findByEmail(String email);

    /**
     * Verify email address uniqueness.
     *
     * @param email the email address to check
     * @return true if matches, false otherwise
     */
    boolean existsByEmail(String email);
}
