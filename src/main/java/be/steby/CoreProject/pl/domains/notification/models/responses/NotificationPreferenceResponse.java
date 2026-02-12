package be.steby.CoreProject.pl.domains.notification.models.responses;

import be.steby.CoreProject.dl.entities.NotificationPreference;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;

import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO for notification preference data.
 *
 * <p>Represents a single preference entry (type + channel combination)
 * as returned by the API.</p>
 *
 * @param notificationType the notification type
 * @param channel          the delivery channel
 * @param enabled          whether this channel is enabled for this type
 * @param quietHoursStart  start of quiet hours (may be null)
 * @param quietHoursEnd    end of quiet hours (may be null)
 * @param digestEnabled    whether digest mode is enabled
 * @param digestFrequency  digest frequency if enabled (may be null)
 */
public record NotificationPreferenceResponse(
        NotificationType notificationType,
        NotificationChannel channel,
        boolean enabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        boolean digestEnabled,
        String digestFrequency
) {

    // =========================================================================
    // Factory Methods
    // =========================================================================

    /**
     * Creates a response from a preference entity.
     *
     * @param preference the preference entity
     * @return the response DTO
     */
    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.getNotificationType(),
                preference.getChannel(),
                preference.isEnabled(),
                preference.getQuietHoursStart(),
                preference.getQuietHoursEnd(),
                preference.isDigestEnabled(),
                preference.getDigestFrequency() != null
                        ? preference.getDigestFrequency().name()
                        : null
        );
    }

    /**
     * Creates a list of responses from preference entities.
     *
     * @param preferences the preference entities
     * @return list of response DTOs
     */
    public static List<NotificationPreferenceResponse> from(List<NotificationPreference> preferences) {
        return preferences.stream()
                .map(NotificationPreferenceResponse::from)
                .toList();
    }
}