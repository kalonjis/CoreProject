package be.steby.CoreProject.bll.domains.notification.models;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationPriority;
import be.steby.CoreProject.dl.enums.notification.NotificationType;

import java.time.Instant;
import java.util.Set;

/**
 * Request object for creating a notification.
 *
 * <p>This record encapsulates all data needed to create and dispatch a notification.
 * It serves as the input to {@code NotificationService.send()} and abstracts
 * away the details of notification entity creation.</p>
 *
 * <h4>Required vs Optional Fields:</h4>
 * <ul>
 *   <li><b>Required:</b> recipient, type, title</li>
 *   <li><b>Optional:</b> All other fields have sensible defaults</li>
 * </ul>
 *
 * <h4>Channel Selection:</h4>
 * <p>If {@code channels} is null or empty, the system will determine appropriate
 * channels based on the notification type and user preferences. Explicitly specifying
 * channels overrides user preferences (use with caution).</p>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Simple notification with defaults
 * NotificationRequest request = NotificationRequest.builder()
 *     .recipient(user)
 *     .type(NotificationType.REMINDER)
 *     .title("Event starting soon")
 *     .body("Team meeting in 15 minutes")
 *     .build();
 *
 * // Detailed notification with all options
 * NotificationRequest request = NotificationRequest.builder()
 *     .recipient(user)
 *     .type(NotificationType.SECURITY)
 *     .priority(NotificationPriority.URGENT)
 *     .title("New login detected")
 *     .body("A new device logged into your account")
 *     .actionUrl("/account/security/sessions")
 *     .data("{\"deviceId\": \"abc123\", \"location\": \"Paris\"}")
 *     .channels(Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
 *     .sourceDomain("auth")
 *     .sourceReferenceId("session-xyz")
 *     .build();
 * }</pre>
 *
 * @param recipient         the user to notify (required)
 * @param type              the notification type (required)
 * @param priority          priority level (optional, defaults to type's default)
 * @param title             short notification title (required)
 * @param body              detailed message body (optional)
 * @param actionUrl         URL to navigate when clicked (optional)
 * @param data              JSON string with additional context (optional)
 * @param icon              custom icon identifier (optional)
 * @param channels          explicit channels to use (optional, null = use preferences)
 * @param scheduledFor      future delivery time (optional, null = immediate)
 * @param expiresAt         expiration time (optional)
 * @param sourceDomain      originating domain (optional, for tracking)
 * @param sourceReferenceId originating entity ID (optional, for tracking)
 *
 * @see NotificationType
 * @see NotificationPriority
 * @see NotificationChannel
 */
public record NotificationRequest(
        User recipient,
        NotificationType type,
        NotificationPriority priority,
        String title,
        String body,
        String actionUrl,
        String data,
        String icon,
        Set<NotificationChannel> channels,
        Instant scheduledFor,
        Instant expiresAt,
        String sourceDomain,
        String sourceReferenceId
) {

    // =========================================================================
    // Validation
    // =========================================================================

    /**
     * Compact constructor with validation.
     */
    public NotificationRequest {
        if (recipient == null) {
            throw new IllegalArgumentException("Notification recipient cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Notification title cannot be null or empty");
        }
        if (title.length() > 150) {
            throw new IllegalArgumentException("Notification title cannot exceed 150 characters");
        }
        if (body != null && body.length() > 500) {
            throw new IllegalArgumentException("Notification body cannot exceed 500 characters");
        }

        // Default priority to type's default if not specified
        if (priority == null) {
            priority = type.getDefaultPriority();
        }
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if this notification should be delivered immediately.
     *
     * @return true if not scheduled for the future
     */
    public boolean isImmediate() {
        return scheduledFor == null || !scheduledFor.isAfter(Instant.now());
    }

    /**
     * Checks if specific channels were requested.
     *
     * @return true if channels were explicitly specified
     */
    public boolean hasExplicitChannels() {
        return channels != null && !channels.isEmpty();
    }

    /**
     * Checks if this notification has a source reference.
     *
     * @return true if source tracking info is present
     */
    public boolean hasSourceReference() {
        return sourceDomain != null && sourceReferenceId != null;
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Creates a new builder for NotificationRequest.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for NotificationRequest.
     *
     * <p>Provides a fluent API for constructing notification requests
     * with only the desired fields specified.</p>
     */
    public static class Builder {
        private User recipient;
        private NotificationType type;
        private NotificationPriority priority;
        private String title;
        private String body;
        private String actionUrl;
        private String data;
        private String icon;
        private Set<NotificationChannel> channels;
        private Instant scheduledFor;
        private Instant expiresAt;
        private String sourceDomain;
        private String sourceReferenceId;

        public Builder recipient(User recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder priority(NotificationPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder actionUrl(String actionUrl) {
            this.actionUrl = actionUrl;
            return this;
        }

        public Builder data(String data) {
            this.data = data;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder channels(Set<NotificationChannel> channels) {
            this.channels = channels;
            return this;
        }

        public Builder channel(NotificationChannel channel) {
            this.channels = Set.of(channel);
            return this;
        }

        public Builder scheduledFor(Instant scheduledFor) {
            this.scheduledFor = scheduledFor;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder sourceDomain(String sourceDomain) {
            this.sourceDomain = sourceDomain;
            return this;
        }

        public Builder sourceReferenceId(String sourceReferenceId) {
            this.sourceReferenceId = sourceReferenceId;
            return this;
        }

        public Builder source(String domain, String referenceId) {
            this.sourceDomain = domain;
            this.sourceReferenceId = referenceId;
            return this;
        }

        public NotificationRequest build() {
            return new NotificationRequest(
                    recipient,
                    type,
                    priority,
                    title,
                    body,
                    actionUrl,
                    data,
                    icon,
                    channels,
                    scheduledFor,
                    expiresAt,
                    sourceDomain,
                    sourceReferenceId
            );
        }
    }

    // =========================================================================
    // Factory Methods
    // =========================================================================

    /**
     * Creates a simple notification request with minimal parameters.
     *
     * @param recipient the user to notify
     * @param type      the notification type
     * @param title     the notification title
     * @return a new notification request
     */
    public static NotificationRequest simple(User recipient, NotificationType type, String title) {
        return builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .build();
    }

    /**
     * Creates a notification request with title and body.
     *
     * @param recipient the user to notify
     * @param type      the notification type
     * @param title     the notification title
     * @param body      the notification body
     * @return a new notification request
     */
    public static NotificationRequest withBody(
            User recipient,
            NotificationType type,
            String title,
            String body) {
        return builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .build();
    }

    /**
     * Creates an actionable notification request.
     *
     * @param recipient the user to notify
     * @param type      the notification type
     * @param title     the notification title
     * @param body      the notification body
     * @param actionUrl the action URL
     * @return a new notification request
     */
    public static NotificationRequest actionable(
            User recipient,
            NotificationType type,
            String title,
            String body,
            String actionUrl) {
        return builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .build();
    }

    /**
     * Creates a scheduled notification request.
     *
     * @param recipient    the user to notify
     * @param type         the notification type
     * @param title        the notification title
     * @param body         the notification body
     * @param scheduledFor when to deliver
     * @return a new notification request
     */
    public static NotificationRequest scheduled(
            User recipient,
            NotificationType type,
            String title,
            String body,
            Instant scheduledFor) {
        return builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .scheduledFor(scheduledFor)
                .build();
    }
}