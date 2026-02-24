package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationStatus;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Notification} entity persistence operations.
 *
 * <p>Provides data access methods for managing notifications, including
 * queries for retrieving user notifications, counting unread notifications,
 * and bulk status updates.</p>
 *
 * <h4>Query Optimization:</h4>
 * <p>Most queries filter by recipient to leverage the database index on
 * {@code recipient_id}. Pagination is recommended for list queries to
 * handle users with many notifications.</p>
 *
 * <h4>Common Query Patterns:</h4>
 * <ul>
 *   <li><b>Unread notifications:</b> Status = DELIVERED</li>
 *   <li><b>Active notifications:</b> Not dismissed, not expired</li>
 *   <li><b>Pending delivery:</b> Status = PENDING and ready for delivery</li>
 * </ul>
 *
 * @see Notification
 * @see NotificationStatus
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // =========================================================================
    // Find by Public ID
    // =========================================================================

    /**
     * Finds a notification by its public ID.
     *
     * @param publicId the notification's public UUID
     * @return the notification if found
     */
    Optional<Notification> findByPublicId(String publicId);

    /**
     * Finds a notification by public ID and recipient.
     *
     * <p>Used to ensure a user can only access their own notifications.</p>
     *
     * @param publicId  the notification's public UUID
     * @param recipient the expected recipient
     * @return the notification if found and owned by recipient
     */
    Optional<Notification> findByPublicIdAndRecipient(String publicId, User recipient);

    // =========================================================================
    // Find by Recipient
    // =========================================================================

    List<Notification> findAllByRecipient(User recipient);

    /**
     * Finds all notifications for a recipient, ordered by creation date (newest first).
     *
     * @param recipient the notification recipient
     * @param pageable  pagination parameters
     * @return page of notifications
     */
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    /**
     * Finds all notifications for a recipient with a specific status.
     *
     * @param recipient the notification recipient
     * @param status    the notification status
     * @param pageable  pagination parameters
     * @return page of notifications
     */
    Page<Notification> findByRecipientAndStatusOrderByCreatedAtDesc(
            User recipient,
            NotificationStatus status,
            Pageable pageable
    );

    /**
     * Finds all notifications for a recipient of a specific type.
     *
     * @param recipient the notification recipient
     * @param type      the notification type
     * @param pageable  pagination parameters
     * @return page of notifications
     */
    Page<Notification> findByRecipientAndTypeOrderByCreatedAtDesc(
            User recipient,
            NotificationType type,
            Pageable pageable
    );

    /**
     * Finds active (not dismissed) notifications for a recipient.
     *
     * @param recipient the notification recipient
     * @param pageable  pagination parameters
     * @return page of active notifications
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient = :recipient
              AND n.status <> 'DISMISSED'
              AND (n.expiresAt IS NULL OR n.expiresAt > CURRENT_TIMESTAMP)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findActiveByRecipient(@Param("recipient") User recipient, Pageable pageable);

    // =========================================================================
    // Unread Notifications
    // =========================================================================

    /**
     * Finds all unread notifications for a recipient.
     *
     * <p>Unread notifications are those with status DELIVERED (sent and not yet read).</p>
     *
     * @param recipient the notification recipient
     * @return list of unread notifications, newest first
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient = :recipient
              AND n.status = 'DELIVERED'
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findUnreadByRecipient(@Param("recipient") User recipient);

    /**
     * Counts unread notifications for a recipient.
     *
     * <p>Used for displaying the notification badge count.</p>
     *
     * @param recipient the notification recipient
     * @return count of unread notifications
     */
    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.recipient = :recipient
              AND n.status = 'DELIVERED'
            """)
    long countUnreadByRecipient(@Param("recipient") User recipient);

    /**
     * Checks if recipient has any unread notifications.
     *
     * <p>More efficient than counting when only existence check is needed.</p>
     *
     * @param recipient the notification recipient
     * @return true if there are unread notifications
     */
    @Query("""
            SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
            FROM Notification n
            WHERE n.recipient = :recipient
              AND n.status = 'DELIVERED'
            """)
    boolean hasUnreadNotifications(@Param("recipient") User recipient);

    // =========================================================================
    // Pending Delivery
    // =========================================================================

    /**
     * Finds notifications pending delivery.
     *
     * <p>Returns notifications that are ready to be sent: PENDING status,
     * not expired, and either not scheduled or past scheduled time.</p>
     *
     * @return list of notifications ready for delivery
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.status = 'PENDING'
              AND (n.expiresAt IS NULL OR n.expiresAt > CURRENT_TIMESTAMP)
              AND (n.scheduledFor IS NULL OR n.scheduledFor <= CURRENT_TIMESTAMP)
            ORDER BY n.priority DESC, n.createdAt ASC
            """)
    List<Notification> findPendingDelivery();

    /**
     * Finds scheduled notifications ready to be sent.
     *
     * <p>Used by scheduler to process notifications at their scheduled time.</p>
     *
     * @param now current timestamp
     * @return list of notifications whose scheduled time has passed
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.status = 'PENDING'
              AND n.scheduledFor IS NOT NULL
              AND n.scheduledFor <= :now
              AND (n.expiresAt IS NULL OR n.expiresAt > :now)
            ORDER BY n.scheduledFor ASC
            """)
    List<Notification> findScheduledReadyForDelivery(@Param("now") Instant now);

    // =========================================================================
    // Bulk Status Updates
    // =========================================================================

    /**
     * Marks all delivered notifications as read for a recipient.
     *
     * <p>Used for "Mark all as read" functionality.</p>
     *
     * @param recipient the notification recipient
     * @param readAt    timestamp to set as read time
     * @return number of notifications updated
     */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.status = 'READ', n.readAt = :readAt
            WHERE n.recipient = :recipient
              AND n.status = 'DELIVERED'
            """)
    int markAllAsRead(@Param("recipient") User recipient, @Param("readAt") Instant readAt);

    /**
     * Marks all notifications as dismissed for a recipient.
     *
     * <p>Used for "Clear all notifications" functionality.</p>
     *
     * @param recipient   the notification recipient
     * @param dismissedAt timestamp to set as dismissed time
     * @return number of notifications updated
     */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.status = 'DISMISSED', n.dismissedAt = :dismissedAt
            WHERE n.recipient = :recipient
              AND n.status IN ('DELIVERED', 'READ')
            """)
    int dismissAll(@Param("recipient") User recipient, @Param("dismissedAt") Instant dismissedAt);

    // =========================================================================
    // Cleanup Operations
    // =========================================================================

    /**
     * Deletes expired notifications.
     *
     * <p>Used by cleanup job to remove old notifications that were never delivered.</p>
     *
     * @param expiredBefore delete notifications expired before this time
     * @return number of notifications deleted
     */
    @Modifying
    @Query("""
            DELETE FROM Notification n
            WHERE n.expiresAt IS NOT NULL
              AND n.expiresAt < :expiredBefore
              AND n.status = 'PENDING'
            """)
    int deleteExpiredPending(@Param("expiredBefore") Instant expiredBefore);

    /**
     * Deletes old dismissed notifications.
     *
     * <p>Used by cleanup job to remove notifications dismissed long ago.</p>
     *
     * @param dismissedBefore delete notifications dismissed before this time
     * @return number of notifications deleted
     */
    @Modifying
    @Query("""
            DELETE FROM Notification n
            WHERE n.status = 'DISMISSED'
              AND n.dismissedAt < :dismissedBefore
            """)
    int deleteOldDismissed(@Param("dismissedBefore") Instant dismissedBefore);

    // =========================================================================
    // Source Reference Queries
    // =========================================================================

    /**
     * Finds notifications by source reference.
     *
     * <p>Useful for finding all notifications related to a specific
     * source entity (e.g., a calendar event).</p>
     *
     * @param sourceDomain      the source domain (e.g., "calendar")
     * @param sourceReferenceId the source entity's public ID
     * @return list of related notifications
     */
    List<Notification> findBySourceDomainAndSourceReferenceId(
            String sourceDomain,
            String sourceReferenceId
    );

    /**
     * Deletes notifications by source reference.
     *
     * <p>Used when the source entity is deleted (e.g., calendar event cancelled).</p>
     *
     * @param sourceDomain      the source domain
     * @param sourceReferenceId the source entity's public ID
     * @return number of notifications deleted
     */
    @Modifying
    int deleteBySourceDomainAndSourceReferenceId(
            String sourceDomain,
            String sourceReferenceId
    );

    // =========================================================================
    // Statistics
    // =========================================================================

    /**
     * Counts notifications by type for a recipient.
     *
     * <p>Used for analytics and preference insights.</p>
     *
     * @param recipient the notification recipient
     * @param type      the notification type
     * @return count of notifications
     */
    long countByRecipientAndType(User recipient, NotificationType type);

    /**
     * Counts notifications created after a specific date for a recipient.
     *
     * @param recipient the notification recipient
     * @param after     count notifications created after this time
     * @return count of notifications
     */
    long countByRecipientAndCreatedAtAfter(User recipient, Instant after);
}