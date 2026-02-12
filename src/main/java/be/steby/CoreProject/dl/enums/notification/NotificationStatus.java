package be.steby.CoreProject.dl.enums.notification;

/**
 * Enumeration of notification lifecycle statuses.
 *
 * <p>Tracks the state of a notification from creation through delivery
 * and user interaction. The status is primarily used for IN_APP notifications
 * to track read/unread state and for delivery tracking across all channels.</p>
 *
 * <h4>Status Lifecycle:</h4>
 * <pre>
 *                    ┌─────────────┐
 *                    │   PENDING   │
 *                    └──────┬──────┘
 *                           │
 *              ┌────────────┼────────────┐
 *              │            │            │
 *              ▼            ▼            ▼
 *       ┌──────────┐  ┌──────────┐  ┌──────────┐
 *       │   SENT   │  │ DELIVERED│  │  FAILED  │
 *       └────┬─────┘  └────┬─────┘  └──────────┘
 *            │             │
 *            └──────┬──────┘
 *                   │
 *                   ▼
 *            ┌──────────┐
 *            │   READ   │
 *            └────┬─────┘
 *                 │
 *                 ▼
 *          ┌───────────┐
 *          │ DISMISSED │
 *          └───────────┘
 * </pre>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Mark notification as read
 * if (notification.getStatus() == NotificationStatus.DELIVERED) {
 *     notification.setStatus(NotificationStatus.READ);
 *     notification.setReadAt(Instant.now());
 * }
 *
 * // Count unread notifications
 * long unreadCount = notifications.stream()
 *     .filter(n -> n.getStatus().isUnread())
 *     .count();
 * }</pre>
 *
 * @see Notification
 */
public enum NotificationStatus {

    // =========================================================================
    // Pre-Delivery Statuses
    // =========================================================================

    /**
     * Notification created but not yet sent.
     *
     * <p>Initial status when a notification is created and queued for delivery.
     * The notification is waiting to be processed by the delivery system.</p>
     *
     * <p><b>Next possible statuses:</b> SENT, FAILED</p>
     */
    PENDING("Pending", false, false),

    // =========================================================================
    // Delivery Statuses
    // =========================================================================

    /**
     * Notification has been sent to the delivery channel.
     *
     * <p>The notification has been handed off to the delivery mechanism
     * (SSE emitter, email service, etc.) but delivery confirmation
     * has not been received.</p>
     *
     * <p>For IN_APP notifications via SSE, this means the event was emitted.
     * For EMAIL, this means the email was accepted by the SMTP server.</p>
     *
     * <p><b>Next possible statuses:</b> DELIVERED, FAILED</p>
     */
    SENT("Sent", false, false),

    /**
     * Notification successfully delivered to the recipient.
     *
     * <p>Confirmation that the notification reached its destination.
     * For IN_APP, this means the client acknowledged receipt.
     * For EMAIL, this may require delivery confirmation (not always available).</p>
     *
     * <p><b>Next possible statuses:</b> READ, DISMISSED</p>
     */
    DELIVERED("Delivered", false, true),

    /**
     * Notification delivery failed.
     *
     * <p>The notification could not be delivered through the intended channel.
     * Reasons may include: user offline (SSE), invalid email address,
     * service unavailable, rate limiting, etc.</p>
     *
     * <p>Failed notifications may be retried depending on the failure reason
     * and retry policy configuration.</p>
     *
     * <p><b>Terminal status</b> (unless retried)</p>
     */
    FAILED("Failed", false, false),

    // =========================================================================
    // User Interaction Statuses
    // =========================================================================

    /**
     * Notification has been read/seen by the user.
     *
     * <p>The user has explicitly or implicitly acknowledged the notification.
     * This could be through clicking on it, viewing the notification center,
     * or programmatic marking as read.</p>
     *
     * <p><b>Next possible statuses:</b> DISMISSED</p>
     */
    READ("Read", true, true),

    /**
     * Notification has been dismissed by the user.
     *
     * <p>The user has explicitly removed the notification from their view.
     * Dismissed notifications are typically hidden from the notification
     * center but may still be accessible in history.</p>
     *
     * <p><b>Terminal status</b></p>
     */
    DISMISSED("Dismissed", true, true);

    // =========================================================================
    // Fields
    // =========================================================================

    private final String displayName;
    private final boolean read;
    private final boolean delivered;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates a new notification status.
     *
     * @param displayName human-readable name for UI display
     * @param read        whether notification has been read in this status
     * @param delivered   whether notification was successfully delivered
     */
    NotificationStatus(String displayName, boolean read, boolean delivered) {
        this.displayName = displayName;
        this.read = read;
        this.delivered = delivered;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /**
     * Gets the human-readable display name.
     *
     * @return the display name for UI purposes
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if the notification has been read in this status.
     *
     * @return true if the notification is considered read
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Checks if the notification was successfully delivered.
     *
     * @return true if delivery was confirmed
     */
    public boolean isDelivered() {
        return delivered;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if the notification is unread.
     *
     * <p>Unread notifications are those that have been delivered
     * but not yet read or dismissed by the user.</p>
     *
     * @return true if the notification is unread
     */
    public boolean isUnread() {
        return this == DELIVERED || this == SENT;
    }

    /**
     * Checks if this is a terminal status.
     *
     * <p>Terminal statuses indicate the end of the notification lifecycle.
     * No further status transitions are expected.</p>
     *
     * @return true if this is a terminal status
     */
    public boolean isTerminal() {
        return this == DISMISSED || this == FAILED;
    }

    /**
     * Checks if the notification is still pending delivery.
     *
     * @return true if notification has not been sent yet
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * Checks if this status indicates a delivery problem.
     *
     * @return true if delivery failed
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * Checks if the notification is actionable by the user.
     *
     * <p>Actionable notifications are those that can be interacted with
     * (marked as read, dismissed, clicked).</p>
     *
     * @return true if user can interact with the notification
     */
    public boolean isActionable() {
        return this == DELIVERED || this == SENT;
    }

    /**
     * Checks if this status should be included in unread count.
     *
     * <p>Used for calculating the notification badge count.</p>
     *
     * @return true if this status counts as unread for badge purposes
     */
    public boolean countsAsUnread() {
        return this == DELIVERED;
    }
}