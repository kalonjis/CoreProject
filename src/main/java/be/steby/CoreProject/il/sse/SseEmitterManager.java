package be.steby.CoreProject.il.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Server-Sent Events (SSE) emitter connections for users.
 *
 * <p>This component maintains a registry of active SSE connections, mapping
 * user public IDs to their {@link SseEmitter} instances. It handles connection
 * lifecycle including registration, retrieval, and cleanup.</p>
 *
 * <h4>Connection Lifecycle:</h4>
 * <pre>
 * 1. User opens app → Frontend calls /api/notifications/stream
 * 2. Controller creates SseEmitter → registers via register()
 * 3. User connected → getEmitter() returns the emitter
 * 4. User closes tab → onCompletion/onTimeout/onError → remove()
 * </pre>
 *
 * <h4>Thread Safety:</h4>
 * <p>Uses {@link ConcurrentHashMap} for thread-safe operations. Multiple
 * threads may simultaneously register, retrieve, and remove emitters.</p>
 *
 * <h4>Single Connection per User:</h4>
 * <p>Currently supports one connection per user. If a user opens multiple
 * tabs, only the most recent connection is maintained. Future enhancement
 * could support multiple connections per user using a list.</p>
 *
 * <h4>Memory Management:</h4>
 * <p>Emitters are automatically removed when:</p>
 * <ul>
 *   <li>Connection completes normally</li>
 *   <li>Connection times out (default: 30 minutes)</li>
 *   <li>An error occurs</li>
 *   <li>User explicitly disconnects</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // In controller - register new connection
 * SseEmitter emitter = new SseEmitter(timeout);
 * sseEmitterManager.register(userPublicId, emitter);
 *
 * // In notification service - check if user connected
 * Optional<SseEmitter> emitter = sseEmitterManager.getEmitter(userPublicId);
 * if (emitter.isPresent()) {
 *     emitter.get().send(event);
 * }
 * }</pre>
 *
 * @see SseEmitter
 * @see SseNotificationPusher
 */
@Component
@Slf4j
public class SseEmitterManager {

    /**
     * Map of user public IDs to their SSE emitters.
     *
     * <p>Thread-safe map allowing concurrent access from multiple threads.</p>
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // =========================================================================
    // Registration
    // =========================================================================

    /**
     * Registers an SSE emitter for a user.
     *
     * <p>If the user already has an active connection, it is completed
     * and replaced with the new one. This handles the case where a user
     * opens a new tab or refreshes the page.</p>
     *
     * <p>Automatically configures callbacks to remove the emitter when
     * the connection ends (completion, timeout, or error).</p>
     *
     * @param userPublicId the user's public ID
     * @param emitter      the SSE emitter to register
     */
    public void register(String userPublicId, SseEmitter emitter) {
        // Complete existing connection if any
        SseEmitter existing = emitters.get(userPublicId);
        if (existing != null) {
            log.debug("Replacing existing SSE connection for user: {}", userPublicId);
            completeQuietly(existing);
        }

        // Configure cleanup callbacks
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for user: {}", userPublicId);
            remove(userPublicId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out for user: {}", userPublicId);
            remove(userPublicId);
        });

        emitter.onError(throwable -> {
            log.debug("SSE connection error for user {}: {}", userPublicId, throwable.getMessage());
            remove(userPublicId);
        });

        // Register the new emitter
        emitters.put(userPublicId, emitter);
        log.info("SSE connection registered for user: {} (total connections: {})",
                userPublicId, emitters.size());
    }

    // =========================================================================
    // Retrieval
    // =========================================================================

    /**
     * Gets the SSE emitter for a user.
     *
     * @param userPublicId the user's public ID
     * @return optional containing the emitter if user is connected
     */
    public Optional<SseEmitter> getEmitter(String userPublicId) {
        return Optional.ofNullable(emitters.get(userPublicId));
    }

    /**
     * Checks if a user is currently connected via SSE.
     *
     * @param userPublicId the user's public ID
     * @return true if user has an active SSE connection
     */
    public boolean isConnected(String userPublicId) {
        return emitters.containsKey(userPublicId);
    }

    // =========================================================================
    // Removal
    // =========================================================================

    /**
     * Removes the SSE emitter for a user.
     *
     * <p>Called automatically by the emitter callbacks, but can also
     * be called manually (e.g., when user logs out).</p>
     *
     * @param userPublicId the user's public ID
     */
    public void remove(String userPublicId) {
        SseEmitter removed = emitters.remove(userPublicId);
        if (removed != null) {
            log.debug("SSE connection removed for user: {} (remaining connections: {})",
                    userPublicId, emitters.size());
        }
    }

    /**
     * Removes and completes the SSE emitter for a user.
     *
     * <p>Use this when you want to explicitly close the connection
     * (e.g., user logout, session invalidation).</p>
     *
     * @param userPublicId the user's public ID
     */
    public void disconnect(String userPublicId) {
        SseEmitter emitter = emitters.remove(userPublicId);
        if (emitter != null) {
            completeQuietly(emitter);
            log.info("SSE connection disconnected for user: {}", userPublicId);
        }
    }

    // =========================================================================
    // Bulk Operations
    // =========================================================================

    /**
     * Gets the number of active SSE connections.
     *
     * @return count of connected users
     */
    public int getConnectionCount() {
        return emitters.size();
    }

    /**
     * Checks if there are any active connections.
     *
     * @return true if at least one user is connected
     */
    public boolean hasConnections() {
        return !emitters.isEmpty();
    }

    /**
     * Disconnects all users.
     *
     * <p>Used during shutdown or for maintenance purposes.</p>
     */
    public void disconnectAll() {
        log.info("Disconnecting all SSE connections (count: {})", emitters.size());
        emitters.forEach((userId, emitter) -> completeQuietly(emitter));
        emitters.clear();
    }

    /**
     * Iterates over all connected emitters.
     *
     * <p>Used for bulk operations like heartbeats or broadcasts.
     * The consumer receives the user ID and emitter for each connection.</p>
     *
     * <p>If the consumer throws an exception for a particular emitter,
     * iteration continues to the next emitter.</p>
     *
     * @param consumer the action to perform for each emitter
     */
    public void forEachEmitter(EmitterConsumer consumer) {
        emitters.forEach((userId, emitter) -> {
            try {
                consumer.accept(userId, emitter);
            } catch (Exception e) {
                log.debug("Error in emitter consumer for user {}: {}", userId, e.getMessage());
            }
        });
    }

    /**
     * Functional interface for emitter iteration.
     */
    @FunctionalInterface
    public interface EmitterConsumer {
        void accept(String userPublicId, SseEmitter emitter) throws Exception;
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Completes an emitter without throwing exceptions.
     */
    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            // Ignore - connection may already be closed
            log.trace("Error completing SSE emitter: {}", e.getMessage());
        }
    }
}