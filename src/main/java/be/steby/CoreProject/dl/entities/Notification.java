package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationPriority;
import be.steby.CoreProject.dl.enums.notification.NotificationStatus;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a notification in the system.
 *
 * <p>A notification is a message sent to a user through one or more delivery
 * channels. This entity tracks the notification content, delivery status,
 * and user interaction state.</p>
 *
 * <h4>Lifecycle:</h4>
 * <ol>
 *   <li><b>Creation:</b> Notification created with PENDING status</li>
 *   <li><b>Delivery:</b> Sent through configured channels, status updated</li>
 *   <li><b>Interaction:</b> User reads or dismisses the notification</li>
 * </ol>
 *
 * <h4>Channel Delivery:</h4>
 * <p>A single notification can be delivered through multiple channels
 * (e.g., both IN_APP and EMAIL). The {@link #channels} field tracks
 * which channels were used for delivery.</p>
 *
 * <h4>Design Decisions:</h4>
 * <ul>
 *   <li><b>Recipient by publicId:</b> Uses user's publicId rather than entity
 *       reference to avoid coupling and simplify queries</li>
 *   <li><b>JSON data field:</b> Flexible storage for notification-specific
 *       context that can be used for deep linking or display customization</li>
 *   <li><b>Soft state tracking:</b> Status reflects overall notification state,
 *       not per-channel delivery status</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * Notification notification = Notification.builder()
 *     .recipientId(user.getPublicId())
 *     .type(NotificationType.REMINDER)
 *     .priority(NotificationPriority.NORMAL)
 *     .title("Event Starting Soon")
 *     .body("Team meeting starts in 15 minutes")
 *     .actionUrl("/calendar/events/abc123")
 *     .build();
 *
 * notificationRepository.save(notification);
 * }</pre>
 *
 * @see NotificationType
 * @see NotificationStatus
 * @see NotificationChannel
 * @see NotificationPriority
 */
@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
                @Index(name = "idx_notification_recipient_status", columnList = "recipient_id, status"),
                @Index(name = "idx_notification_recipient_read", columnList = "recipient_id, read_at"),
                @Index(name = "idx_notification_type", columnList = "type"),
                @Index(name = "idx_notification_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Notification extends BaseEntity<Long> {

    // =========================================================================
    // Recipient Information
    // =========================================================================

    /**
     * The user who should receive this notification.
     *
     * <p>Unidirectional relationship - User entity does not reference
     * notifications to avoid circular dependencies. Queries for user's
     * notifications go through {@code NotificationRepository}.</p>
     *
     * <p>Uses LAZY fetching to avoid loading user data when not needed.
     * The internal ID is used for FK constraint and efficient JOINs,
     * while publicId is used when exposing data through the API.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // =========================================================================
    // Notification Classification
    // =========================================================================

    /**
     * Type of notification.
     *
     * <p>Determines default behavior, channels, and display style.
     * See {@link NotificationType} for available types.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    /**
     * Priority level of the notification.
     *
     * <p>Affects delivery urgency and whether quiet hours are bypassed.
     * If not specified, defaults to the type's default priority.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * Current status in the notification lifecycle.
     *
     * <p>Tracks whether the notification has been sent, delivered,
     * read, or dismissed.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    // =========================================================================
    // Notification Content
    // =========================================================================

    /**
     * Short title of the notification.
     *
     * <p>Displayed prominently in notification toasts, badges, and lists.
     * Should be concise and immediately informative.</p>
     */
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    /**
     * Main body/message of the notification.
     *
     * <p>Provides additional context beyond the title. May be truncated
     * in some display contexts (e.g., push notifications).</p>
     */
    @Column(name = "body", length = 500)
    private String body;

    /**
     * URL for the notification action/deep link.
     *
     * <p>When the user clicks the notification, they should be directed
     * to this URL. Can be a relative path within the application.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code /calendar/events/abc123}</li>
     *   <li>{@code /account/security}</li>
     *   <li>{@code /messages/inbox/xyz789}</li>
     * </ul>
     */
    @Column(name = "action_url", length = 255)
    private String actionUrl;

    /**
     * Additional contextual data in JSON format.
     *
     * <p>Flexible storage for notification-specific data that may be
     * needed for display customization or action handling.</p>
     *
     * <p>Example content:</p>
     * <pre>{@code
     * {
     *   "eventId": "abc123",
     *   "eventTitle": "Team Meeting",
     *   "startsAt": "2025-02-11T14:00:00Z"
     * }
     * }</pre>
     */
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    /**
     * Icon identifier for the notification.
     *
     * <p>Optional icon name or path to customize the notification
     * appearance. If not specified, a default icon based on the
     * notification type will be used.</p>
     */
    @Column(name = "icon", length = 100)
    private String icon;

    // =========================================================================
    // Delivery Information
    // =========================================================================

    /**
     * Channels through which this notification was/will be delivered.
     *
     * <p>A notification may be sent through multiple channels simultaneously.
     * The actual channels used depend on user preferences and notification type.</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "notification_channel",
            joinColumns = @JoinColumn(name = "notification_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 10)
    @Builder.Default
    private Set<NotificationChannel> channels = new HashSet<>();

    /**
     * Timestamp when the notification was sent.
     *
     * <p>Set when the notification transitions from PENDING to SENT status.</p>
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Timestamp when delivery was confirmed.
     *
     * <p>Set when at least one channel confirms successful delivery.
     * For IN_APP notifications, this is when the SSE event was acknowledged.</p>
     */
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    // =========================================================================
    // User Interaction
    // =========================================================================

    /**
     * Timestamp when the user read/viewed the notification.
     *
     * <p>Set when the user explicitly or implicitly acknowledges the notification.
     * Used for tracking engagement and calculating unread counts.</p>
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Timestamp when the user dismissed the notification.
     *
     * <p>Set when the user explicitly removes the notification from their view.
     * Dismissed notifications are typically hidden but not deleted.</p>
     */
    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    // =========================================================================
    // Source Tracking
    // =========================================================================

    /**
     * Source domain that generated this notification.
     *
     * <p>Identifies which part of the system created the notification,
     * useful for analytics and debugging.</p>
     *
     * <p>Examples: "calendar", "auth", "account", "social"</p>
     */
    @Column(name = "source_domain", length = 50)
    private String sourceDomain;

    /**
     * Reference ID to the source entity that triggered the notification.
     *
     * <p>Optional reference to the originating entity's publicId,
     * allowing traceability back to the source event.</p>
     *
     * <p>Examples: event publicId, order publicId, etc.</p>
     */
    @Column(name = "source_reference_id", length = 36)
    private String sourceReferenceId;

    // =========================================================================
    // Scheduling
    // =========================================================================

    /**
     * Scheduled delivery time for delayed notifications.
     *
     * <p>If set, the notification should not be delivered until this time.
     * Used for scheduled reminders and timed notifications.</p>
     */
    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    /**
     * Expiration time after which the notification is no longer relevant.
     *
     * <p>If set and the current time exceeds this value, the notification
     * should not be delivered and can be automatically cleaned up.</p>
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if the notification has been read.
     *
     * @return true if the notification has been read
     */
    public boolean isRead() {
        return readAt != null;
    }

    /**
     * Checks if the notification has been dismissed.
     *
     * @return true if the notification has been dismissed
     */
    public boolean isDismissed() {
        return dismissedAt != null;
    }

    /**
     * Checks if the notification is still pending delivery.
     *
     * @return true if not yet sent
     */
    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    /**
     * Checks if the notification has expired.
     *
     * @return true if the notification has passed its expiration time
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if the notification is scheduled for future delivery.
     *
     * @return true if scheduled for a future time
     */
    public boolean isScheduled() {
        return scheduledFor != null && Instant.now().isBefore(scheduledFor);
    }

    /**
     * Checks if the notification should be delivered now.
     *
     * <p>A notification is ready for delivery if it's pending,
     * not expired, and either not scheduled or past its scheduled time.</p>
     *
     * @return true if ready for immediate delivery
     */
    public boolean isReadyForDelivery() {
        return isPending() && !isExpired() && !isScheduled();
    }

    /**
     * Marks the notification as sent.
     *
     * @param timestamp the time when sent
     */
    public void markAsSent(Instant timestamp) {
        this.status = NotificationStatus.SENT;
        this.sentAt = timestamp;
    }

    /**
     * Marks the notification as delivered.
     *
     * @param timestamp the time when delivery was confirmed
     */
    public void markAsDelivered(Instant timestamp) {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = timestamp;
    }

    /**
     * Marks the notification as read.
     *
     * @param timestamp the time when read
     */
    public void markAsRead(Instant timestamp) {
        this.status = NotificationStatus.READ;
        this.readAt = timestamp;
    }

    /**
     * Marks the notification as dismissed.
     *
     * @param timestamp the time when dismissed
     */
    public void markAsDismissed(Instant timestamp) {
        this.status = NotificationStatus.DISMISSED;
        this.dismissedAt = timestamp;
    }

    /**
     * Marks the notification as failed.
     */
    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }

    /**
     * Adds a delivery channel to this notification.
     *
     * @param channel the channel to add
     */
    public void addChannel(NotificationChannel channel) {
        if (this.channels == null) {
            this.channels = new HashSet<>();
        }
        this.channels.add(channel);
    }

    /**
     * Checks if this notification was delivered through a specific channel.
     *
     * @param channel the channel to check
     * @return true if the notification used this channel
     */
    public boolean wasDeliveredThrough(NotificationChannel channel) {
        return this.channels != null && this.channels.contains(channel);
    }
}