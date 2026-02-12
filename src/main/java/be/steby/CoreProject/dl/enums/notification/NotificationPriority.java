package be.steby.CoreProject.dl.enums.notification;

/**
 * Enumeration of notification priority levels.
 *
 * <p>Priority determines how urgently a notification should be delivered
 * and how prominently it should be displayed to the user. Higher priority
 * notifications may bypass certain user preferences (like quiet hours)
 * and use more intrusive delivery channels.</p>
 *
 * <h4>Priority Impact:</h4>
 * <table border="1">
 *   <tr>
 *     <th>Priority</th>
 *     <th>Quiet Hours</th>
 *     <th>Digest</th>
 *     <th>Sound</th>
 *     <th>Typical Use</th>
 *   </tr>
 *   <tr>
 *     <td>LOW</td>
 *     <td>Respected</td>
 *     <td>Can batch</td>
 *     <td>Silent</td>
 *     <td>Info, tips</td>
 *   </tr>
 *   <tr>
 *     <td>NORMAL</td>
 *     <td>Respected</td>
 *     <td>Individual</td>
 *     <td>Default</td>
 *     <td>Reminders, social</td>
 *   </tr>
 *   <tr>
 *     <td>HIGH</td>
 *     <td>May bypass</td>
 *     <td>Immediate</td>
 *     <td>Attention</td>
 *     <td>Alerts, actions needed</td>
 *   </tr>
 *   <tr>
 *     <td>URGENT</td>
 *     <td>Always bypass</td>
 *     <td>Immediate</td>
 *     <td>Urgent</td>
 *     <td>Security, critical</td>
 *   </tr>
 * </table>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Check if notification should bypass quiet hours
 * if (notification.getPriority().bypassesQuietHours()) {
 *     deliverImmediately(notification);
 * } else if (userPreferences.isInQuietHours()) {
 *     scheduleForLater(notification);
 * }
 *
 * // Sort notifications by priority
 * notifications.sort(Comparator.comparing(
 *     n -> n.getPriority().getWeight(),
 *     Comparator.reverseOrder()
 * ));
 * }</pre>
 *
 * @see NotificationType
 * @see Notification
 */
public enum NotificationPriority {

    // =========================================================================
    // Priority Levels (ascending order)
    // =========================================================================

    /**
     * Low priority notification.
     *
     * <p>Non-urgent information that can wait. These notifications may be
     * batched into digest emails and will always respect quiet hours.
     * They should not interrupt the user's workflow.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li>Can be batched in daily/weekly digests</li>
     *   <li>Silent delivery (no sound)</li>
     *   <li>Respects all user preferences</li>
     *   <li>May be deprioritized when many notifications are pending</li>
     * </ul>
     *
     * <h5>Examples:</h5>
     * <ul>
     *   <li>New feature announcements</li>
     *   <li>Tips and recommendations</li>
     *   <li>Non-essential confirmations</li>
     * </ul>
     */
    LOW("Low", 1, false, true),

    /**
     * Normal priority notification.
     *
     * <p>Standard notifications that are relevant but not urgent.
     * Delivered promptly but respect user preferences for quiet hours
     * and channel selection.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li>Delivered individually (not batched)</li>
     *   <li>Default notification sound</li>
     *   <li>Respects quiet hours</li>
     *   <li>Standard visual prominence</li>
     * </ul>
     *
     * <h5>Examples:</h5>
     * <ul>
     *   <li>Calendar reminders</li>
     *   <li>Social interactions</li>
     *   <li>System updates</li>
     * </ul>
     */
    NORMAL("Normal", 2, false, false),

    /**
     * High priority notification.
     *
     * <p>Important notifications that require prompt attention.
     * May bypass some user preferences to ensure timely delivery.
     * Should be used sparingly to maintain user trust.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li>Immediate delivery</li>
     *   <li>Attention-grabbing sound</li>
     *   <li>May bypass quiet hours (configurable)</li>
     *   <li>Enhanced visual prominence</li>
     * </ul>
     *
     * <h5>Examples:</h5>
     * <ul>
     *   <li>Payment issues requiring attention</li>
     *   <li>Expiring subscriptions</li>
     *   <li>Action-required alerts</li>
     * </ul>
     */
    HIGH("High", 3, true, false),

    /**
     * Urgent priority notification.
     *
     * <p>Critical notifications that must reach the user immediately.
     * These bypass all timing restrictions and may use multiple channels
     * simultaneously to ensure delivery. Reserved for security-critical
     * and time-sensitive matters.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li>Immediate multi-channel delivery</li>
     *   <li>Urgent notification sound</li>
     *   <li>Always bypasses quiet hours</li>
     *   <li>Maximum visual prominence</li>
     *   <li>May trigger additional channels (SMS)</li>
     * </ul>
     *
     * <h5>Examples:</h5>
     * <ul>
     *   <li>Security alerts (new login, password change)</li>
     *   <li>Account compromise detected</li>
     *   <li>Critical system failures</li>
     * </ul>
     */
    URGENT("Urgent", 4, true, false);

    // =========================================================================
    // Fields
    // =========================================================================

    private final String displayName;
    private final int weight;
    private final boolean bypassQuietHours;
    private final boolean canBeBatched;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates a new notification priority.
     *
     * @param displayName      human-readable name for UI display
     * @param weight           numeric weight for sorting (higher = more urgent)
     * @param bypassQuietHours whether this priority bypasses quiet hours
     * @param canBeBatched     whether notifications can be batched in digests
     */
    NotificationPriority(String displayName, int weight, boolean bypassQuietHours, boolean canBeBatched) {
        this.displayName = displayName;
        this.weight = weight;
        this.bypassQuietHours = bypassQuietHours;
        this.canBeBatched = canBeBatched;
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
     * Gets the numeric weight for sorting purposes.
     *
     * <p>Higher values indicate higher priority. Can be used to sort
     * notifications with most urgent first.</p>
     *
     * @return the priority weight (1-4)
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Checks if this priority bypasses quiet hours.
     *
     * <p>Some priorities are important enough that they should be delivered
     * even during the user's configured quiet hours.</p>
     *
     * @return true if quiet hours should be ignored
     */
    public boolean bypassesQuietHours() {
        return bypassQuietHours;
    }

    /**
     * Checks if notifications with this priority can be batched.
     *
     * <p>Low priority notifications may be collected and sent together
     * in a daily or weekly digest email.</p>
     *
     * @return true if batching is allowed
     */
    public boolean canBeBatched() {
        return canBeBatched;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if this priority is higher than another.
     *
     * @param other the priority to compare with
     * @return true if this priority is higher
     */
    public boolean isHigherThan(NotificationPriority other) {
        return this.weight > other.weight;
    }

    /**
     * Checks if this priority is at least as high as the specified level.
     *
     * @param minimum the minimum priority level
     * @return true if this priority meets or exceeds the minimum
     */
    public boolean isAtLeast(NotificationPriority minimum) {
        return this.weight >= minimum.weight;
    }

    /**
     * Checks if this is considered a high-priority notification.
     *
     * <p>High-priority notifications (HIGH or URGENT) may receive
     * special handling in the delivery pipeline.</p>
     *
     * @return true if priority is HIGH or URGENT
     */
    public boolean isHighPriority() {
        return this == HIGH || this == URGENT;
    }

    /**
     * Checks if sound should be played for this priority.
     *
     * @return true if notification sound should be played
     */
    public boolean shouldPlaySound() {
        return this != LOW;
    }

    /**
     * Gets the recommended maximum delivery delay for this priority.
     *
     * <p>Returns the maximum acceptable delay in seconds before
     * the notification loses its relevance.</p>
     *
     * @return maximum delay in seconds
     */
    public int getMaxDeliveryDelaySeconds() {
        return switch (this) {
            case URGENT -> 5;
            case HIGH -> 30;
            case NORMAL -> 300;   // 5 minutes
            case LOW -> 3600;     // 1 hour
        };
    }
}