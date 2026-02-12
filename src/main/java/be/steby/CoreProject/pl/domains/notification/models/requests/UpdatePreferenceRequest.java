package be.steby.CoreProject.pl.domains.notification.models.requests;

import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO for updating a single notification preference.
 *
 * <h4>Usage:</h4>
 * <pre>{@code
 * PUT /api/notifications/preferences
 * {
 *   "notificationType": "REMINDER",
 *   "channel": "EMAIL",
 *   "enabled": false
 * }
 * }</pre>
 *
 * @param notificationType the notification type to configure
 * @param channel          the channel to configure
 * @param enabled          whether to enable or disable
 */
public record UpdatePreferenceRequest(
        @NotNull(message = "Notification type is required")
        NotificationType notificationType,

        @NotNull(message = "Channel is required")
        NotificationChannel channel,

        @NotNull(message = "Enabled status is required")
        Boolean enabled
) {}