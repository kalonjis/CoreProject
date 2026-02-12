package be.steby.CoreProject.il.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;

/**
 * Service for pushing notifications to users via SSE.
 *
 * <p>This component provides a high-level API for sending notifications
 * to connected users through Server-Sent Events. It handles serialization,
 * error handling, and connection management.</p>
 *
 * <h4>Event Format:</h4>
 * <p>Notifications are sent as SSE events with:</p>
 * <ul>
 *   <li><b>event:</b> "notification" (event type for client filtering)</li>
 *   <li><b>data:</b> JSON payload with notification details</li>
 * </ul>
 *
 * <h4>Client-Side Handling:</h4>
 * <pre>{@code
 * const eventSource = new EventSource('/api/notifications/stream');
 *
 * eventSource.addEventListener('notification', (event) => {
 *     const notification = JSON.parse(event.data);
 *     showToast(notification);
 *     updateBadgeCount();
 * });
 * }</pre>
 *
 * <h4>Error Handling:</h4>
 * <p>If sending fails (e.g., connection broken), the emitter is automatically
 * removed and the method returns false. The notification is still persisted
 * in the database and will be visible when the user reconnects.</p>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Push to single user
 * boolean delivered = ssePusher.pushToUser(userPublicId, payload);
 *
 * // Push to multiple users
 * ssePusher.pushToUsers(userIds, payload);
 *
 * // Broadcast to all connected users
 * ssePusher.broadcast(payload);
 * }</pre>
 *
 * @see SseEmitterManager
 * @see SseEmitter
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseNotificationPusher {

    private final SseEmitterManager emitterManager;
    private final ObjectMapper objectMapper;

    /**
     * SSE event name for notifications.
     *
     * <p>Clients should listen for this event type:</p>
     * <pre>{@code
     * eventSource.addEventListener('notification', handler);
     * }</pre>
     */
    public static final String EVENT_NOTIFICATION = "notification";

    /**
     * SSE event name for connection heartbeat.
     */
    public static final String EVENT_HEARTBEAT = "heartbeat";

    // =========================================================================
    // Single User Push
    // =========================================================================

    /**
     * Pushes a notification to a single user.
     *
     * @param userPublicId the user's public ID
     * @param payload      the notification payload (will be serialized to JSON)
     * @return true if the notification was successfully sent, false if user not connected
     */
    public boolean pushToUser(String userPublicId, Object payload) {
        return emitterManager.getEmitter(userPublicId)
                .map(emitter -> sendEvent(userPublicId, emitter, EVENT_NOTIFICATION, payload))
                .orElseGet(() -> {
                    log.debug("User {} not connected via SSE, notification not pushed",
                            userPublicId);
                    return false;
                });
    }

    /**
     * Pushes a notification to a single user with custom event type.
     *
     * @param userPublicId the user's public ID
     * @param eventType    the SSE event type
     * @param payload      the notification payload
     * @return true if sent successfully
     */
    public boolean pushToUser(String userPublicId, String eventType, Object payload) {
        return emitterManager.getEmitter(userPublicId)
                .map(emitter -> sendEvent(userPublicId, emitter, eventType, payload))
                .orElse(false);
    }

    // =========================================================================
    // Multi User Push
    // =========================================================================

    /**
     * Pushes a notification to multiple users.
     *
     * @param userPublicIds the users' public IDs
     * @param payload       the notification payload
     * @return number of users who received the notification
     */
    public int pushToUsers(Set<String> userPublicIds, Object payload) {
        int delivered = 0;
        for (String userId : userPublicIds) {
            if (pushToUser(userId, payload)) {
                delivered++;
            }
        }
        log.debug("Pushed notification to {}/{} users", delivered, userPublicIds.size());
        return delivered;
    }

    // =========================================================================
    // Broadcast
    // =========================================================================

    /**
     * Broadcasts a notification to all connected users.
     *
     * <p>Use with caution - this sends to ALL connected users.
     * Suitable for system-wide announcements.</p>
     *
     * @param payload the notification payload
     * @return number of users who received the notification
     */
    public int broadcast(Object payload) {
        int connectionCount = emitterManager.getConnectionCount();
        if (connectionCount == 0) {
            log.debug("No connected users, broadcast skipped");
            return 0;
        }

        log.debug("Broadcasting notification to {} connected users", connectionCount);

        final int[] delivered = {0};
        String jsonData;
        try {
            jsonData = serializePayload(payload);
        } catch (Exception e) {
            log.error("Failed to serialize broadcast payload: {}", e.getMessage());
            return 0;
        }

        emitterManager.forEachEmitter((userId, emitter) -> {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name(EVENT_NOTIFICATION)
                        .data(jsonData);
                emitter.send(event);
                delivered[0]++;
            } catch (IOException e) {
                log.debug("Failed to broadcast to user {}: {}", userId, e.getMessage());
            }
        });

        log.info("Broadcast delivered to {}/{} users", delivered[0], connectionCount);
        return delivered[0];
    }

    // =========================================================================
    // Heartbeat
    // =========================================================================

    /**
     * Sends a heartbeat to a user to keep the connection alive.
     *
     * <p>Useful for preventing proxy timeouts on long-lived connections.</p>
     *
     * @param userPublicId the user's public ID
     * @return true if heartbeat sent successfully
     */
    public boolean sendHeartbeat(String userPublicId) {
        return emitterManager.getEmitter(userPublicId)
                .map(emitter -> sendEvent(userPublicId, emitter, EVENT_HEARTBEAT, "ping"))
                .orElse(false);
    }

    // =========================================================================
    // Connection Status
    // =========================================================================

    /**
     * Checks if a user is currently connected via SSE.
     *
     * @param userPublicId the user's public ID
     * @return true if user has an active SSE connection
     */
    public boolean isUserConnected(String userPublicId) {
        return emitterManager.isConnected(userPublicId);
    }

    /**
     * Gets the total number of connected users.
     *
     * @return count of SSE connections
     */
    public int getConnectedUserCount() {
        return emitterManager.getConnectionCount();
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Sends an SSE event to an emitter.
     *
     * @return true if sent successfully
     */
    private boolean sendEvent(String userPublicId, SseEmitter emitter, String eventType, Object payload) {
        try {
            String jsonData = serializePayload(payload);

            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name(eventType)
                    .data(jsonData);

            emitter.send(event);

            log.trace("SSE event '{}' sent to user {}", eventType, userPublicId);
            return true;

        } catch (IOException e) {
            log.debug("Failed to send SSE event to user {}: {} - removing connection",
                    userPublicId, e.getMessage());
            emitterManager.remove(userPublicId);
            return false;
        }
    }

    /**
     * Serializes a payload to JSON string.
     */
    private String serializePayload(Object payload) {
        if (payload instanceof String) {
            return (String) payload;
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SSE payload: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize notification payload", e);
        }
    }
}