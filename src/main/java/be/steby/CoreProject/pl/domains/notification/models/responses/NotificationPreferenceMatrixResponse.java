package be.steby.CoreProject.pl.domains.notification.models.responses;

import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;

import java.time.LocalTime;
import java.util.Map;

/**
 * Response DTO for the complete preference matrix.
 *
 * <p>Represents all preferences for a user as a matrix of
 * type → channel → enabled status.</p>
 *
 * <h4>JSON Structure:</h4>
 * <pre>{@code
 * {
 *   "preferences": {
 *     "REMINDER": {
 *       "IN_APP": true,
 *       "EMAIL": false,
 *       "PUSH": true,
 *       "SMS": false
 *     },
 *     "SECURITY": {
 *       "IN_APP": true,
 *       "EMAIL": true,
 *       "PUSH": true,
 *       "SMS": true
 *     }
 *     // ... other types
 *   },
 *   "quietHours": {
 *     "PUSH": {
 *       "start": "22:00",
 *       "end": "08:00"
 *     }
 *   }
 * }
 * }</pre>
 *
 * @param preferences matrix of type → channel → enabled
 * @param quietHours  quiet hours per channel
 */
public record NotificationPreferenceMatrixResponse(
        Map<NotificationType, Map<NotificationChannel, Boolean>> preferences,
        Map<NotificationChannel, QuietHoursResponse> quietHours
) {

    /**
     * Quiet hours configuration for a channel.
     *
     * @param start quiet hours start time
     * @param end   quiet hours end time
     */
    public record QuietHoursResponse(LocalTime start, LocalTime end) {}
}