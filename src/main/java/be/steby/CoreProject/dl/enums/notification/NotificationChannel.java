package be.steby.CoreProject.dl.enums.notification;

/**
 * Enumeration of available notification delivery channels.
 *
 * <p>Each channel represents a distinct method of delivering notifications
 * to users. Channels have different characteristics regarding delivery speed,
 * reliability, user reach, and intrusiveness.</p>
 *
 * <h4>Channel Selection Strategy:</h4>
 * <p>The appropriate channel(s) for a notification depend on:</p>
 * <ul>
 *   <li><b>Urgency:</b> How quickly must the user be informed?</li>
 *   <li><b>User state:</b> Is the user currently active in the application?</li>
 *   <li><b>User preferences:</b> Which channels has the user enabled?</li>
 *   <li><b>Notification type:</b> What are the default channels for this type?</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Check if channel requires external service
 * if (channel.requiresExternalService()) {
 *     externalServiceClient.send(notification);
 * }
 *
 * // Get all real-time capable channels
 * Set<NotificationChannel> realtimeChannels = Arrays.stream(NotificationChannel.values())
 *     .filter(NotificationChannel::isRealTime)
 *     .collect(Collectors.toSet());
 * }</pre>
 *
 * @see NotificationType
 * @see NotificationPreference
 */
public enum NotificationChannel {

    // =========================================================================
    // In-Application Channels
    // =========================================================================

    /**
     * In-app notification delivered via SSE (Server-Sent Events).
     *
     * <p>Notifications appear directly in the application UI as toasts,
     * badges, or in the notification center. Requires the user to have
     * the application open in their browser.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li><b>Delivery:</b> Instant (via SSE connection)</li>
     *   <li><b>Reach:</b> Only when app is open</li>
     *   <li><b>Persistence:</b> Stored in database, visible in notification center</li>
     *   <li><b>User control:</b> Can be marked as read, dismissed</li>
     * </ul>
     *
     * <h5>Best for:</h5>
     * <ul>
     *   <li>Real-time updates while user is active</li>
     *   <li>Non-critical information</li>
     *   <li>Frequent notifications that shouldn't spam email</li>
     * </ul>
     */
    IN_APP("In-App", true, false),

    // =========================================================================
    // External Channels
    // =========================================================================

    /**
     * Email notification channel.
     *
     * <p>Notifications sent to the user's registered email address.
     * Suitable for important information that should be preserved
     * and accessible outside the application.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li><b>Delivery:</b> Asynchronous (seconds to minutes)</li>
     *   <li><b>Reach:</b> Anytime (user checks email independently)</li>
     *   <li><b>Persistence:</b> Permanent in user's mailbox</li>
     *   <li><b>User control:</b> Managed via email client</li>
     * </ul>
     *
     * <h5>Best for:</h5>
     * <ul>
     *   <li>Security notifications (password changes, new logins)</li>
     *   <li>Important confirmations (orders, registrations)</li>
     *   <li>Detailed information requiring formatted content</li>
     *   <li>Legal or compliance notifications</li>
     * </ul>
     */
    EMAIL("Email", false, true),

    /**
     * Web Push notification channel.
     *
     * <p>Browser push notifications that appear at the OS level,
     * even when the application tab is closed. Requires user permission
     * and browser support.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li><b>Delivery:</b> Near-instant</li>
     *   <li><b>Reach:</b> When browser is running (tab can be closed)</li>
     *   <li><b>Persistence:</b> Temporary (OS notification center)</li>
     *   <li><b>User control:</b> Can revoke permission anytime</li>
     * </ul>
     *
     * <h5>Best for:</h5>
     * <ul>
     *   <li>Time-sensitive alerts</li>
     *   <li>Re-engagement notifications</li>
     *   <li>Updates when user is away from the app</li>
     * </ul>
     *
     * <h5>Requirements:</h5>
     * <ul>
     *   <li>HTTPS connection</li>
     *   <li>Service Worker registered</li>
     *   <li>User granted notification permission</li>
     *   <li>VAPID keys configured</li>
     * </ul>
     */
    PUSH("Push", true, true),

    /**
     * SMS notification channel.
     *
     * <p>Text messages sent to the user's verified phone number.
     * Reserved for critical notifications due to cost and intrusiveness.</p>
     *
     * <h5>Characteristics:</h5>
     * <ul>
     *   <li><b>Delivery:</b> Near-instant</li>
     *   <li><b>Reach:</b> Anytime (phone doesn't need internet)</li>
     *   <li><b>Persistence:</b> Permanent in SMS app</li>
     *   <li><b>Cost:</b> Per-message billing</li>
     * </ul>
     *
     * <h5>Best for:</h5>
     * <ul>
     *   <li>Two-factor authentication codes</li>
     *   <li>Critical security alerts</li>
     *   <li>Time-sensitive confirmations</li>
     * </ul>
     *
     * <h5>Requirements:</h5>
     * <ul>
     *   <li>Verified phone number</li>
     *   <li>SMS provider configured (e.g., Twilio)</li>
     *   <li>User consent for SMS communications</li>
     * </ul>
     */
    SMS("SMS", false, true);

    // =========================================================================
    // Fields
    // =========================================================================

    private final String displayName;
    private final boolean realTime;
    private final boolean requiresExternalService;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates a new notification channel.
     *
     * @param displayName             human-readable name for UI display
     * @param realTime                whether this channel delivers in real-time
     * @param requiresExternalService whether this channel needs external service
     */
    NotificationChannel(String displayName, boolean realTime, boolean requiresExternalService) {
        this.displayName = displayName;
        this.realTime = realTime;
        this.requiresExternalService = requiresExternalService;
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
     * Checks if this channel delivers notifications in real-time.
     *
     * <p>Real-time channels can deliver notifications instantly while
     * the user is connected. Non-real-time channels have inherent delays.</p>
     *
     * @return true if notifications are delivered in real-time
     */
    public boolean isRealTime() {
        return realTime;
    }

    /**
     * Checks if this channel requires an external service.
     *
     * <p>External services include email providers (SMTP), SMS gateways,
     * or push notification services. These may have associated costs,
     * rate limits, and availability concerns.</p>
     *
     * @return true if an external service is required
     */
    public boolean requiresExternalService() {
        return requiresExternalService;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if this channel is available without user being in the app.
     *
     * <p>Some channels can reach users even when they're not actively
     * using the application, making them suitable for important notifications.</p>
     *
     * @return true if channel can reach user outside the app
     */
    public boolean canReachOfflineUser() {
        return this != IN_APP;
    }

    /**
     * Checks if this channel has associated per-message costs.
     *
     * <p>Channels with costs should be used judiciously and may have
     * additional rate limiting or user consent requirements.</p>
     *
     * @return true if using this channel incurs costs
     */
    public boolean hasCost() {
        return this == SMS;
    }

    /**
     * Checks if this channel requires explicit user permission.
     *
     * <p>Some channels require the user to explicitly grant permission
     * before notifications can be sent through them.</p>
     *
     * @return true if explicit permission is required
     */
    public boolean requiresPermission() {
        return this == PUSH || this == SMS;
    }

    /**
     * Checks if this channel supports rich content (HTML, images).
     *
     * @return true if rich content is supported
     */
    public boolean supportsRichContent() {
        return this == EMAIL || this == IN_APP;
    }
}