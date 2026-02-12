package be.steby.CoreProject.pl.domains.notification.models.responses;

import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationPriority;
import be.steby.CoreProject.dl.enums.notification.NotificationStatus;
import be.steby.CoreProject.dl.enums.notification.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Response DTO for notification data.
 *
 * <p>Represents a notification as returned by the API. Contains all
 * information needed by the frontend to display and interact with
 * the notification.</p>
 *
 * <h4>Usage:</h4>
 * <pre>{@code
 * // Single notification
 * GET /api/notifications/{publicId}
 * → NotificationResponse
 *
 * // List of notifications
 * GET /api/notifications
 * → Page<NotificationResponse>
 * }</pre>
 *
 * @param publicId       unique identifier for the notification
 * @param type           notification type (REMINDER, ALERT, etc.)
 * @param priority       priority level (LOW, NORMAL, HIGH, URGENT)
 * @param status         current status (PENDING, DELIVERED, READ, etc.)
 * @param title          notification title
 * @param body           notification body/message (may be null)
 * @param actionUrl      URL to navigate on click (may be null)
 * @param icon           icon identifier (may be null)
 * @param channels       channels through which notification was sent
 * @param read           whether the notification has been read
 * @param dismissed      whether the notification has been dismissed
 * @param createdAt      when the notification was created
 * @param readAt         when the notification was read (may be null)
 * @param sourceDomain   originating domain (may be null)
 */
public record NotificationResponse(
        String publicId,
        NotificationType type,
        NotificationPriority priority,
        NotificationStatus status,
        String title,
        String body,
        String actionUrl,
        String icon,
        Set<NotificationChannel> channels,
        boolean read,
        boolean dismissed,
        Instant createdAt,
        Instant readAt,
        String sourceDomain
) {

    // =========================================================================
    // Factory Methods
    // =========================================================================

    /**
     * Creates a response from a notification entity.
     *
     * @param notification the notification entity
     * @return the response DTO
     */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getPublicId(),
                notification.getType(),
                notification.getPriority(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getBody(),
                notification.getActionUrl(),
                notification.getIcon(),
                notification.getChannels(),
                notification.isRead(),
                notification.isDismissed(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getSourceDomain()
        );
    }

    /**
     * Creates a list of responses from notification entities.
     *
     * @param notifications the notification entities
     * @return list of response DTOs
     */
    public static List<NotificationResponse> from(List<Notification> notifications) {
        return notifications.stream()
                .map(NotificationResponse::from)
                .toList();
    }
}