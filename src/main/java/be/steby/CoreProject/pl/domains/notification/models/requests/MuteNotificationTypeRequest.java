package be.steby.CoreProject.pl.domains.notification.models.requests;

import be.steby.CoreProject.dl.enums.notification.NotificationType;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for muting/unmuting a notification type.
 *
 * <h4>Usage:</h4>
 * <pre>{@code
 * POST /api/notifications/preferences/mute
 * {
 *   "notificationType": "SOCIAL",
 *   "muted": true
 * }
 * }</pre>
 *
 * <p>Muting disables all channels for the specified notification type.
 * Unmuting re-enables all channels to their default state.</p>
 *
 * @param notificationType the notification type to mute/unmute
 * @param muted            true to mute (disable all channels), false to unmute
 */
public record MuteNotificationTypeRequest(
        @NotNull(message = "Notification type is required")
        NotificationType notificationType,

        @NotNull(message = "Muted status is required")
        Boolean muted
) {}