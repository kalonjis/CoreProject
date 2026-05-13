package be.steby.CoreProject.bll.domains.notification.listeners;

import be.steby.CoreProject.bll.domains.notification.events.NotificationCreatedEvent;
import be.steby.CoreProject.dal.repositories.NotificationRepository;
import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.il.sse.SseNotificationPusher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Listener for in-app notification delivery via SSE.
 *
 * <p>Handles real-time notification delivery to users' browsers through
 * Server-Sent Events (SSE). This listener is <b>synchronous</b> to ensure
 * the notification is pushed before the service method returns.</p>
 *
 * <h4>Why Synchronous?</h4>
 * <ul>
 *   <li>SSE push is very fast (~1-5ms)</li>
 *   <li>Caller knows notification was delivered in real-time</li>
 *   <li>Simpler error handling and status tracking</li>
 * </ul>
 *
 * <h4>Execution Order:</h4>
 * <p>Uses {@code @Order(1)} to execute before async listeners, ensuring
 * the user sees the notification immediately while email/push are sent
 * in background.</p>
 *
 * <h4>Connection Handling:</h4>
 * <p>If the user is not currently connected via SSE, the push is silently
 * skipped. The notification is already persisted and will appear in the
 * notification center when the user returns to the app.</p>
 *
 * @see NotificationCreatedEvent
 * @see SseNotificationPusher
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationListener {

    private final SseNotificationPusher ssePusher;
    private final NotificationRepository notificationRepository;

    /**
     * Handles notification delivery via SSE.
     *
     * <p>This method is synchronous (no @Async) and executes first (@Order(1))
     * to provide immediate feedback to connected users.</p>
     *
     * @param event the notification created event
     */
    @EventListener
    @Order(1)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        // Check if IN_APP channel is requested
        if (!event.shouldDeliverVia(NotificationChannel.IN_APP)) {
            return;
        }

        Notification notification = event.notification();
        String recipientId = event.getRecipientPublicId();

        log.debug("Pushing in-app notification {} to user {}",
                event.getNotificationPublicId(), recipientId);

        try {
            // Build SSE payload
            SseNotificationPayload payload = toPayload(notification);

            // Push via SSE — event ID = createdAt epoch millis (used by Last-Event-ID on reconnect)
            String eventId = String.valueOf(notification.getCreatedAt().toEpochMilli());
            boolean delivered = ssePusher.pushToUser(recipientId, eventId, payload);

            // Update notification status
            Instant now = Instant.now();
            notification.markAsSent(now);

            if (delivered) {
                notification.markAsDelivered(now);
                log.debug("In-app notification {} delivered via SSE",
                        event.getNotificationPublicId());
            } else {
                log.debug("User {} not connected via SSE, notification {} stored for later",
                        recipientId, event.getNotificationPublicId());
            }

            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Failed to push in-app notification {} to user {}: {}",
                    event.getNotificationPublicId(), recipientId, e.getMessage());
            // Don't throw - notification is persisted, user will see it later
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Converts a notification to an SSE payload.
     */
    private SseNotificationPayload toPayload(Notification notification) {
        return new SseNotificationPayload(
                notification.getPublicId(),
                notification.getType().name(),
                notification.getPriority().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getActionUrl(),
                notification.getIcon(),
                notification.getCreatedAt().toString()
        );
    }

    // =========================================================================
    // Payload Record
    // =========================================================================

    /**
     * Lightweight payload for SSE transmission.
     *
     * <p>Contains only the essential data needed for immediate display.
     * Serialized to JSON for SSE event data.</p>
     */
    public record SseNotificationPayload(
            String id,
            String type,
            String priority,
            String title,
            String body,
            String actionUrl,
            String icon,
            String createdAt
    ) {}
}