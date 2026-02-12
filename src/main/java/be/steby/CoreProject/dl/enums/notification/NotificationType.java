package be.steby.CoreProject.dl.enums.notification;

/**
 * Enumeration of notification types in the system.
 *
 * <p>Each notification type represents a category of notification with its own
 * semantic meaning, default behavior, and typical use cases. The type determines
 * how the notification should be displayed, which channels are appropriate by default,
 * and what priority level to assign.</p>
 *
 * <h4>Type Categories:</h4>
 * <ul>
 *   <li><b>User-triggered:</b> CONFIRMATION - Result of user actions</li>
 *   <li><b>Time-triggered:</b> REMINDER - Scheduled notifications</li>
 *   <li><b>System-triggered:</b> ALERT, SECURITY, SYSTEM - Automated notifications</li>
 *   <li><b>Social-triggered:</b> SOCIAL - Interactions from other users</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * Notification notification = Notification.builder()
 *     .type(NotificationType.REMINDER)
 *     .title("Event starting soon")
 *     .body("Team meeting in 15 minutes")
 *     .build();
 *
 * // Check if notification requires immediate attention
 * if (notification.getType().getDefaultPriority() == NotificationPriority.HIGH) {
 *     // Handle urgent notification
 * }
 * }</pre>
 *
 * @see Notification
 * @see NotificationPriority
 * @see NotificationChannel
 */
public enum NotificationType {

    // =========================================================================
    // Time-Based Notifications
    // =========================================================================

    /**
     * Reminder notification for scheduled events or tasks.
     *
     * <p>Triggered by the system based on time conditions, typically before
     * a calendar event or deadline. Users expect these notifications to be
     * timely and actionable.</p>
     *
     * <p><b>Typical triggers:</b></p>
     * <ul>
     *   <li>Calendar event starting in X minutes</li>
     *   <li>Task deadline approaching</li>
     *   <li>Scheduled appointment reminder</li>
     * </ul>
     *
     * <p><b>Default channels:</b> IN_APP, PUSH, EMAIL (based on user preferences)</p>
     * <p><b>Default priority:</b> NORMAL</p>
     */
    REMINDER("Reminder", NotificationPriority.NORMAL),

    // =========================================================================
    // Action Result Notifications
    // =========================================================================

    /**
     * Confirmation notification for completed actions.
     *
     * <p>Sent after a user action has been successfully processed, especially
     * for actions initiated from a different device or asynchronous operations.
     * These provide assurance that the requested action was completed.</p>
     *
     * <p><b>Typical triggers:</b></p>
     * <ul>
     *   <li>Account email verified</li>
     *   <li>Order placed successfully</li>
     *   <li>Settings updated from another device</li>
     * </ul>
     *
     * <p><b>Default channels:</b> IN_APP, EMAIL</p>
     * <p><b>Default priority:</b> LOW</p>
     */
    CONFIRMATION("Confirmation", NotificationPriority.LOW),

    // =========================================================================
    // Attention-Required Notifications
    // =========================================================================

    /**
     * Alert notification requiring user attention or action.
     *
     * <p>Used for important events that may require the user to take action
     * but are not security-critical. These notifications should stand out
     * but not cause alarm.</p>
     *
     * <p><b>Typical triggers:</b></p>
     * <ul>
     *   <li>Payment method expiring soon</li>
     *   <li>Subscription renewal reminder</li>
     *   <li>Required profile update</li>
     * </ul>
     *
     * <p><b>Default channels:</b> IN_APP, PUSH, EMAIL</p>
     * <p><b>Default priority:</b> HIGH</p>
     */
    ALERT("Alert", NotificationPriority.HIGH),

    /**
     * Security notification for account safety matters.
     *
     * <p>Critical notifications related to account security that require
     * immediate user awareness. These should always be delivered through
     * multiple channels to ensure the user is informed.</p>
     *
     * <p><b>Typical triggers:</b></p>
     * <ul>
     *   <li>New device login detected</li>
     *   <li>Password changed</li>
     *   <li>Suspicious activity detected</li>
     *   <li>Two-factor authentication changes</li>
     * </ul>
     *
     * <p><b>Default channels:</b> IN_APP, PUSH, EMAIL, SMS</p>
     * <p><b>Default priority:</b> URGENT</p>
     */
    SECURITY("Security", NotificationPriority.URGENT),

    // =========================================================================
    // Social Notifications
    // =========================================================================

    /**
     * Social notification for user interactions.
     *
     * <p>Notifications triggered by other users' actions that involve
     * the recipient. These foster engagement and community interaction.</p>
     *
     * <p><b>Typical triggers:</b></p>
     * <ul>
     *   <li>New follower</li>
     *   <li>Mention in a comment</li>
     *   <li>Shared document or resource</li>
     *   <li>Invitation to collaborate</li>
     * </ul>
     *
     * <p><b>Default channels:</b> IN_APP, PUSH</p>
     * <p><b>Default priority:</b> NORMAL</p>
     */
    SOCIAL("Social", NotificationPriority.NORMAL),

    // =========================================================================
    // Informational Notifications
    // =========================================================================

    /**
     * Informational notification for general updates.
     *
     * <p>Non-urgent notifications that provide useful information to the user.
     * These can be safely batched or delayed without impacting user experience.</p>
     *
     * <p><b>Typical triggers:</b></p>
     * <ul>
     *   <li>New feature announcement</li>
     *   <li>Tips and recommendations</li>
     *   <li>Weekly summary or digest</li>
     * </ul>
     *
     * <p><b>Default channels:</b> IN_APP, EMAIL</p>
     * <p><b>Default priority:</b> LOW</p>
     */
    INFO("Information", NotificationPriority.LOW),

    /**
     * System notification for platform-level events.
     *
     * <p>Administrative notifications from the platform itself, typically
     * regarding maintenance, policy changes, or technical matters.</p>
     *
     * <p><b>Typical triggers:</b></p>
     * <ul>
     *   <li>Scheduled maintenance announcement</li>
     *   <li>Terms of service update</li>
     *   <li>Service degradation notice</li>
     *   <li>Account status changes by admin</li>
     * </ul>
     *
     * <p><b>Default channels:</b> IN_APP, EMAIL</p>
     * <p><b>Default priority:</b> NORMAL</p>
     */
    SYSTEM("System", NotificationPriority.NORMAL);

    // =========================================================================
    // Fields
    // =========================================================================

    private final String displayName;
    private final NotificationPriority defaultPriority;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates a new notification type.
     *
     * @param displayName     human-readable name for UI display
     * @param defaultPriority default priority level for this type
     */
    NotificationType(String displayName, NotificationPriority defaultPriority) {
        this.displayName = displayName;
        this.defaultPriority = defaultPriority;
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
     * Gets the default priority for this notification type.
     *
     * <p>This priority is used when no explicit priority is specified
     * during notification creation. It can be overridden on a per-notification basis.</p>
     *
     * @return the default priority level
     */
    public NotificationPriority getDefaultPriority() {
        return defaultPriority;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if this notification type is security-related.
     *
     * <p>Security notifications may have special handling requirements,
     * such as mandatory email delivery or audit logging.</p>
     *
     * @return true if this is a security notification
     */
    public boolean isSecurityRelated() {
        return this == SECURITY;
    }

    /**
     * Checks if this notification type is time-sensitive.
     *
     * <p>Time-sensitive notifications should be delivered immediately
     * and may bypass certain batching or digest mechanisms.</p>
     *
     * @return true if this notification type is time-sensitive
     */
    public boolean isTimeSensitive() {
        return this == REMINDER || this == SECURITY || this == ALERT;
    }

    /**
     * Checks if this notification type can be batched in a digest.
     *
     * <p>Some notification types are suitable for daily or weekly digest
     * emails, while others should always be delivered individually.</p>
     *
     * @return true if this type can be included in digest emails
     */
    public boolean canBeDigested() {
        return this == INFO || this == SOCIAL || this == CONFIRMATION;
    }
}