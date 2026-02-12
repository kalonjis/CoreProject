package be.steby.CoreProject.bll.domains.notification.events;

import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;

import java.util.Set;

/**
 * Event published when a notification is created and persisted.
 *
 * <p>This event triggers the delivery of the notification through
 * the appropriate channels. Each channel has its own listener that
 * decides whether to process the notification synchronously or
 * asynchronously.</p>
 *
 * <h4>Event Flow:</h4>
 * <pre>
 * NotificationService.send()
 *       │
 *       ▼
 * Notification persisted
 *       │
 *       ▼
 * NotificationCreatedEvent published
 *       │
 *       ├──▶ InAppNotificationListener (SYNC)
 *       │         └── Push via SSE
 *       │
 *       ├──▶ EmailNotificationListener (ASYNC)
 *       │         └── Send email
 *       │
 *       └──▶ PushNotificationListener (ASYNC) [future]
 *                 └── Send push notification
 * </pre>
 *
 * <h4>Listener Implementation:</h4>
 * <ul>
 *   <li>Listeners check if their channel is in {@code channels} set</li>
 *   <li>SYNC listeners execute in the publishing thread</li>
 *   <li>ASYNC listeners execute in their own thread pool</li>
 *   <li>Use {@code @Order} to control execution order</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * @EventListener
 * @Order(1)
 * public void handleNotificationCreated(NotificationCreatedEvent event) {
 *     if (event.shouldDeliverVia(NotificationChannel.IN_APP)) {
 *         ssePusher.push(event.notification());
 *     }
 * }
 * }</pre>
 *
 * @param notification the persisted notification entity
 * @param channels     the channels through which to deliver
 *
 * @see Notification
 * @see NotificationChannel
 */
public record NotificationCreatedEvent(
        Notification notification,
        Set<NotificationChannel> channels
) {

    // =========================================================================
    // Validation
    // =========================================================================

    /**
     * Compact constructor with validation.
     */
    public NotificationCreatedEvent {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("At least one channel must be specified");
        }
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Checks if the notification should be delivered via a specific channel.
     *
     * @param channel the channel to check
     * @return true if the channel is in the delivery set
     */
    public boolean shouldDeliverVia(NotificationChannel channel) {
        return channels.contains(channel);
    }

    /**
     * Gets the recipient's public ID for logging purposes.
     *
     * @return the recipient's public ID
     */
    public String getRecipientPublicId() {
        return notification.getRecipient().getPublicId();
    }

    /**
     * Gets the notification's public ID for logging purposes.
     *
     * @return the notification's public ID
     */
    public String getNotificationPublicId() {
        return notification.getPublicId();
    }
}