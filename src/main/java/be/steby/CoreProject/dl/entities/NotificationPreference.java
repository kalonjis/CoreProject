package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Entity representing user preferences for notification delivery.
 *
 * <p>Each record defines whether a specific notification type should be
 * delivered through a specific channel for a particular user. This allows
 * fine-grained control over notification delivery.</p>
 *
 * <h4>Preference Matrix:</h4>
 * <p>Users can enable/disable each combination of notification type and channel:</p>
 * <pre>
 * ┌──────────────┬─────────┬────────┬─────────┬───────┐
 * │ Type         │  Email  │  Push  │  In-App │  SMS  │
 * ├──────────────┼─────────┼────────┼─────────┼───────┤
 * │ REMINDER     │   ☐     │   ☑    │    ☑    │   ☐   │
 * │ CONFIRMATION │   ☑     │   ☐    │    ☑    │   ☐   │
 * │ SECURITY     │   ☑     │   ☑    │    ☑    │   ☑   │
 * │ ...          │   ...   │  ...   │   ...   │  ...  │
 * └──────────────┴─────────┴────────┴─────────┴───────┘
 * </pre>
 *
 * <h4>Default Behavior:</h4>
 * <p>If no preference record exists for a type/channel combination,
 * the system should use sensible defaults based on the notification type.
 * This entity only stores explicit user preferences that override defaults.</p>
 *
 * <h4>Quiet Hours:</h4>
 * <p>Users can configure quiet hours during which non-urgent notifications
 * are held for later delivery. These settings are stored per-channel,
 * allowing different quiet hours for push vs email.</p>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Check if user wants email for reminders
 * Optional<NotificationPreference> pref = preferenceRepository
 *     .findByUserIdAndTypeAndChannel(
 *         user.getPublicId(),
 *         NotificationType.REMINDER,
 *         NotificationChannel.EMAIL
 *     );
 *
 * boolean shouldSendEmail = pref
 *     .map(NotificationPreference::isEnabled)
 *     .orElse(true); // Default to enabled
 * }</pre>
 *
 * @see Notification
 * @see NotificationType
 * @see NotificationChannel
 */
@Entity
@Table(name = "notification_preference",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_pref_user_type_channel",
                        columnNames = {"user_id", "notification_type", "channel"}
                )
        },
        indexes = {
                @Index(name = "idx_notification_pref_user", columnList = "user_id"),
                @Index(name = "idx_notification_pref_user_type", columnList = "user_id, notification_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NotificationPreference extends BaseEntity<Long> {

    // =========================================================================
    // User Reference
    // =========================================================================

    /**
     * The user who owns this notification preference.
     *
     * <p>Unidirectional relationship - User entity does not reference
     * preferences to avoid circular dependencies. Queries for user's
     * preferences go through {@code NotificationPreferenceRepository}.</p>
     *
     * <p>Uses LAZY fetching to avoid loading user data when not needed.
     * The internal ID is used for FK constraint and efficient JOINs.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // =========================================================================
    // Preference Target
    // =========================================================================

    /**
     * The notification type this preference applies to.
     *
     * <p>Combined with {@link #channel}, this defines the specific
     * notification delivery path being configured.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationType notificationType;

    /**
     * The delivery channel this preference applies to.
     *
     * <p>Combined with {@link #notificationType}, this defines the specific
     * notification delivery path being configured.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    // =========================================================================
    // Preference Settings
    // =========================================================================

    /**
     * Whether notifications should be delivered through this channel.
     *
     * <p>When false, notifications of this type will not be sent
     * through this channel, regardless of other settings.</p>
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // =========================================================================
    // Quiet Hours Configuration
    // =========================================================================

    /**
     * Start time of quiet hours (inclusive).
     *
     * <p>During quiet hours, non-urgent notifications are held for later
     * delivery. If null, quiet hours are not configured for this preference.</p>
     *
     * <p>Example: 22:00 (10 PM)</p>
     */
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    /**
     * End time of quiet hours (exclusive).
     *
     * <p>Notifications resume delivery after this time.
     * Can be earlier than start time to span midnight.</p>
     *
     * <p>Example: 08:00 (8 AM) - with start at 22:00, quiet hours are 10PM-8AM</p>
     */
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    // =========================================================================
    // Digest/Batching Preferences
    // =========================================================================

    /**
     * Whether to batch notifications into a digest.
     *
     * <p>When enabled, low-priority notifications may be collected
     * and sent together in a periodic digest instead of individually.</p>
     *
     * <p>Only applicable to EMAIL channel.</p>
     */
    @Column(name = "digest_enabled")
    @Builder.Default
    private boolean digestEnabled = false;

    /**
     * Frequency of digest delivery.
     *
     * <p>Defines how often digest emails are sent when {@link #digestEnabled}
     * is true. Values: "DAILY", "WEEKLY"</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "digest_frequency", length = 10)
    private DigestFrequency digestFrequency;

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if quiet hours are currently active.
     *
     * <p>Handles the case where quiet hours span midnight
     * (e.g., 22:00 to 08:00).</p>
     *
     * @param currentTime the current time to check
     * @return true if currently within quiet hours
     */
    public boolean isInQuietHours(LocalTime currentTime) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        // Handle overnight quiet hours (e.g., 22:00 - 08:00)
        if (quietHoursStart.isAfter(quietHoursEnd)) {
            // Quiet hours span midnight
            return currentTime.isAfter(quietHoursStart) || currentTime.isBefore(quietHoursEnd);
        } else {
            // Normal range (e.g., 12:00 - 14:00)
            return !currentTime.isBefore(quietHoursStart) && currentTime.isBefore(quietHoursEnd);
        }
    }

    /**
     * Checks if quiet hours are configured.
     *
     * @return true if quiet hours start and end are both set
     */
    public boolean hasQuietHours() {
        return quietHoursStart != null && quietHoursEnd != null;
    }

    /**
     * Checks if digest mode is fully configured.
     *
     * @return true if digest is enabled with a frequency
     */
    public boolean hasDigestConfigured() {
        return digestEnabled && digestFrequency != null;
    }

    /**
     * Checks if this preference allows immediate delivery.
     *
     * <p>Considers both enabled status and current quiet hours.</p>
     *
     * @param currentTime the current time
     * @return true if notification can be delivered now
     */
    public boolean allowsImmediateDelivery(LocalTime currentTime) {
        return enabled && !isInQuietHours(currentTime);
    }

    // =========================================================================
    // Inner Enums
    // =========================================================================

    /**
     * Frequency options for notification digests.
     */
    public enum DigestFrequency {

        /**
         * Daily digest, typically sent in the morning.
         */
        DAILY("Daily"),

        /**
         * Weekly digest, typically sent on Monday morning.
         */
        WEEKLY("Weekly");

        private final String displayName;

        DigestFrequency(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // =========================================================================
    // Static Factory Methods
    // =========================================================================

    /**
     * Creates a new enabled preference for a user/type/channel combination.
     *
     * @param user             the user entity
     * @param notificationType the notification type
     * @param channel          the delivery channel
     * @return a new enabled preference
     */
    public static NotificationPreference enabled(
            User user,
            NotificationType notificationType,
            NotificationChannel channel) {
        return NotificationPreference.builder()
                .user(user)
                .notificationType(notificationType)
                .channel(channel)
                .enabled(true)
                .build();
    }

    /**
     * Creates a new disabled preference for a user/type/channel combination.
     *
     * @param user             the user entity
     * @param notificationType the notification type
     * @param channel          the delivery channel
     * @return a new disabled preference
     */
    public static NotificationPreference disabled(
            User user,
            NotificationType notificationType,
            NotificationChannel channel) {
        return NotificationPreference.builder()
                .user(user)
                .notificationType(notificationType)
                .channel(channel)
                .enabled(false)
                .build();
    }
}