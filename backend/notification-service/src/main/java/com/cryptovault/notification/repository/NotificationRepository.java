package com.cryptovault.notification.repository;

import com.cryptovault.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * <h3>NotificationRepository</h3>
 *
 * <p><b>Why it exists:</b> Interfaces persistence operations for Notification entities with the database.</p>
 * <p><b>Architectural Layer:</b> Persistence / Repository Layer.</p>
 * <p><b>Design Patterns Used:</b> Repository / Data Mapper Pattern.</p>
 * <p><b>Security Concepts Demonstrated:</b> Scope boundary searches (using userId queries to verify historical data context).</p>
 * <p><b>Future AWS Integration Path:</b> Saves notification logs parsed from incoming SQS events.</p>
 * <p><b>Enterprise Relevance:</b> Abstracts database interactions, decoupling SQL commands from services.</p>
 * <p><b>Interview Talking Points:</b> Inherits CRUD operations from {@link JpaRepository}. Defines custom query methods to retrieve a user's notification list sorted chronologically from newest to oldest.</p>
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Retrieves all notifications sent to a specific user, sorted from newest to oldest.
     *
     * @param userId the user identity UUID
     * @return list of matching notifications
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
