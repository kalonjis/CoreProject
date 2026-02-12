package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.NotificationPreference;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for {@link NotificationPreference} entity persistence operations.
 *
 * <p>Provides data access methods for managing user notification preferences,
 * including queries for checking enabled channels and finding users with
 * specific preference configurations.</p>
 *
 * <h4>Default Behavior:</h4>
 * <p>When no preference record exists for a user/type/channel combination,
 * the system should assume default behavior (typically enabled). This repository
 * only stores explicit user preferences that differ from defaults.</p>
 *
 * <h4>Query Patterns:</h4>
 * <ul>
 *   <li><b>Check if enabled:</b> Find preference, default to true if absent</li>
 *   <li><b>Get all preferences:</b> For displaying preference matrix to user</li>
 *   <li><b>Find users to notify:</b> Bulk query for notification dispatch</li>
 * </ul>
 *
 * @see NotificationPreference
 * @see NotificationType
 * @see NotificationChannel
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    // =========================================================================
    // Single Preference Lookup
    // =========================================================================

    /**
     * Finds a specific preference by user, type, and channel.
     *
     * <p>The unique combination of user + type + channel identifies
     * a single preference setting.</p>
     *
     * @param user             the user
     * @param notificationType the notification type
     * @param channel          the delivery channel
     * @return the preference if explicitly set
     */
    Optional<NotificationPreference> findByUserAndNotificationTypeAndChannel(
            User user,
            NotificationType notificationType,
            NotificationChannel channel
    );

    /**
     * Checks if a preference exists for a user/type/channel combination.
     *
     * @param user             the user
     * @param notificationType the notification type
     * @param channel          the delivery channel
     * @return true if a preference record exists
     */
    boolean existsByUserAndNotificationTypeAndChannel(
            User user,
            NotificationType notificationType,
            NotificationChannel channel
    );

    // =========================================================================
    // User Preferences
    // =========================================================================

    /**
     * Finds all preferences for a user.
     *
     * <p>Used for displaying the full preference matrix to the user.</p>
     *
     * @param user the user
     * @return list of all user's preferences
     */
    List<NotificationPreference> findByUser(User user);

    /**
     * Finds all preferences for a user and specific notification type.
     *
     * <p>Returns preferences across all channels for a given type.</p>
     *
     * @param user             the user
     * @param notificationType the notification type
     * @return list of preferences for the type
     */
    List<NotificationPreference> findByUserAndNotificationType(
            User user,
            NotificationType notificationType
    );

    /**
     * Finds all preferences for a user and specific channel.
     *
     * <p>Returns preferences across all types for a given channel.</p>
     *
     * @param user    the user
     * @param channel the delivery channel
     * @return list of preferences for the channel
     */
    List<NotificationPreference> findByUserAndChannel(User user, NotificationChannel channel);

    // =========================================================================
    // Enabled Channel Queries
    // =========================================================================

    /**
     * Finds enabled channels for a user and notification type.
     *
     * <p>Returns only channels that are explicitly enabled. Channels without
     * a preference record are not included (caller should apply defaults).</p>
     *
     * @param user             the user
     * @param notificationType the notification type
     * @return list of enabled preferences
     */
    @Query("""
            SELECT np FROM NotificationPreference np
            WHERE np.user = :user
              AND np.notificationType = :type
              AND np.enabled = true
            """)
    List<NotificationPreference> findEnabledByUserAndType(
            @Param("user") User user,
            @Param("type") NotificationType notificationType
    );

    /**
     * Gets the set of enabled channels for a user and notification type.
     *
     * @param user             the user
     * @param notificationType the notification type
     * @return set of enabled channels
     */
    @Query("""
            SELECT np.channel FROM NotificationPreference np
            WHERE np.user = :user
              AND np.notificationType = :type
              AND np.enabled = true
            """)
    Set<NotificationChannel> findEnabledChannelsByUserAndType(
            @Param("user") User user,
            @Param("type") NotificationType notificationType
    );

    /**
     * Gets the set of disabled channels for a user and notification type.
     *
     * <p>Useful for determining which default channels should be excluded.</p>
     *
     * @param user             the user
     * @param notificationType the notification type
     * @return set of explicitly disabled channels
     */
    @Query("""
            SELECT np.channel FROM NotificationPreference np
            WHERE np.user = :user
              AND np.notificationType = :type
              AND np.enabled = false
            """)
    Set<NotificationChannel> findDisabledChannelsByUserAndType(
            @Param("user") User user,
            @Param("type") NotificationType notificationType
    );

    // =========================================================================
    // Bulk User Queries (for notification dispatch)
    // =========================================================================

    /**
     * Finds all users who have enabled a specific type/channel combination.
     *
     * <p>Used when sending notifications to multiple users (e.g., system
     * announcements). Only returns users with explicit enabled preferences.</p>
     *
     * @param notificationType the notification type
     * @param channel          the delivery channel
     * @return list of users with enabled preference
     */
    @Query("""
            SELECT np.user FROM NotificationPreference np
            WHERE np.notificationType = :type
              AND np.channel = :channel
              AND np.enabled = true
            """)
    List<User> findUsersWithEnabledPreference(
            @Param("type") NotificationType notificationType,
            @Param("channel") NotificationChannel channel
    );

    /**
     * Finds all users who have NOT disabled a specific type/channel combination.
     *
     * <p>Includes users with no preference record (implicit default enabled)
     * and users with explicit enabled preference. Excludes only users who
     * explicitly disabled the channel.</p>
     *
     * @param notificationType the notification type
     * @param channel          the delivery channel
     * @param userIds          the set of user IDs to check
     * @return list of users who should receive notifications
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.id IN :userIds
              AND NOT EXISTS (
                  SELECT np FROM NotificationPreference np
                  WHERE np.user = u
                    AND np.notificationType = :type
                    AND np.channel = :channel
                    AND np.enabled = false
              )
            """)
    List<User> findUsersNotOptedOut(
            @Param("type") NotificationType notificationType,
            @Param("channel") NotificationChannel channel,
            @Param("userIds") Set<Long> userIds
    );

    // =========================================================================
    // Quiet Hours Queries
    // =========================================================================

    /**
     * Finds preferences with quiet hours configured for a user.
     *
     * @param user the user
     * @return list of preferences with quiet hours
     */
    @Query("""
            SELECT np FROM NotificationPreference np
            WHERE np.user = :user
              AND np.quietHoursStart IS NOT NULL
              AND np.quietHoursEnd IS NOT NULL
            """)
    List<NotificationPreference> findWithQuietHours(@Param("user") User user);

    /**
     * Checks if user has quiet hours configured for a specific channel.
     *
     * @param user    the user
     * @param channel the delivery channel
     * @return true if quiet hours are configured
     */
    @Query("""
            SELECT CASE WHEN COUNT(np) > 0 THEN true ELSE false END
            FROM NotificationPreference np
            WHERE np.user = :user
              AND np.channel = :channel
              AND np.quietHoursStart IS NOT NULL
              AND np.quietHoursEnd IS NOT NULL
            """)
    boolean hasQuietHoursForChannel(@Param("user") User user, @Param("channel") NotificationChannel channel);

    // =========================================================================
    // Digest Preferences
    // =========================================================================

    /**
     * Finds preferences with digest enabled for a user.
     *
     * @param user the user
     * @return list of preferences with digest enabled
     */
    @Query("""
            SELECT np FROM NotificationPreference np
            WHERE np.user = :user
              AND np.digestEnabled = true
            """)
    List<NotificationPreference> findWithDigestEnabled(@Param("user") User user);

    /**
     * Finds all users who have digest enabled for email channel.
     *
     * <p>Used by digest job to find users who should receive digest emails.</p>
     *
     * @param frequency the digest frequency (DAILY, WEEKLY)
     * @return list of users with digest enabled
     */
    @Query("""
            SELECT DISTINCT np.user FROM NotificationPreference np
            WHERE np.channel = 'EMAIL'
              AND np.digestEnabled = true
              AND np.digestFrequency = :frequency
            """)
    List<User> findUsersWithDigestEnabled(
            @Param("frequency") NotificationPreference.DigestFrequency frequency
    );

    // =========================================================================
    // Bulk Operations
    // =========================================================================

    /**
     * Deletes all preferences for a user.
     *
     * <p>Used when user account is deleted.</p>
     *
     * @param user the user
     * @return number of preferences deleted
     */
    @Modifying
    int deleteByUser(User user);

    /**
     * Enables all channels for a notification type for a user.
     *
     * @param user             the user
     * @param notificationType the notification type
     * @return number of preferences updated
     */
    @Modifying
    @Query("""
            UPDATE NotificationPreference np
            SET np.enabled = true
            WHERE np.user = :user
              AND np.notificationType = :type
            """)
    int enableAllChannelsForType(
            @Param("user") User user,
            @Param("type") NotificationType notificationType
    );

    /**
     * Disables all channels for a notification type for a user.
     *
     * <p>Effectively mutes this notification type completely.</p>
     *
     * @param user             the user
     * @param notificationType the notification type
     * @return number of preferences updated
     */
    @Modifying
    @Query("""
            UPDATE NotificationPreference np
            SET np.enabled = false
            WHERE np.user = :user
              AND np.notificationType = :type
            """)
    int disableAllChannelsForType(
            @Param("user") User user,
            @Param("type") NotificationType notificationType
    );

    // =========================================================================
    // Statistics
    // =========================================================================

    /**
     * Counts enabled preferences for a user.
     *
     * @param user the user
     * @return count of enabled preferences
     */
    long countByUserAndEnabledTrue(User user);

    /**
     * Counts disabled preferences for a user.
     *
     * @param user the user
     * @return count of disabled preferences
     */
    long countByUserAndEnabledFalse(User user);
}