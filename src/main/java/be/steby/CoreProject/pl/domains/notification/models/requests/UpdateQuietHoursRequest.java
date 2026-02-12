package be.steby.CoreProject.pl.domains.notification.models.requests;

import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Request DTO for updating quiet hours.
 *
 * <h4>Usage:</h4>
 * <pre>{@code
 * PUT /api/notifications/preferences/quiet-hours
 * {
 *   "channel": "PUSH",
 *   "start": "22:00",
 *   "end": "08:00"
 * }
 * }</pre>
 *
 * <p>Set both start and end to null to disable quiet hours.</p>
 *
 * @param channel the channel to configure
 * @param start   quiet hours start time (null to disable)
 * @param end     quiet hours end time (null to disable)
 */
public record UpdateQuietHoursRequest(
        @NotNull(message = "Channel is required")
        NotificationChannel channel,

        LocalTime start,
        LocalTime end
) {
    /**
     * Validates that both start and end are either set or null.
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return (start == null && end == null) || (start != null && end != null);
    }
}