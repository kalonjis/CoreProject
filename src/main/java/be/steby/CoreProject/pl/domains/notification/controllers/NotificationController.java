package be.steby.CoreProject.pl.domains.notification.controllers;

import be.steby.CoreProject.bll.domains.notification.services.NotificationService;
import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.sse.SseConfig;
import be.steby.CoreProject.il.sse.SseEmitterManager;
import be.steby.CoreProject.pl.domains.notification.models.responses.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * REST controller for notification operations.
 *
 * <p>Provides endpoints for:</p>
 * <ul>
 *   <li>SSE stream subscription for real-time notifications</li>
 *   <li>Listing and querying notifications</li>
 *   <li>Marking notifications as read/dismissed</li>
 *   <li>Deleting notifications</li>
 * </ul>
 *
 * <h4>SSE Connection:</h4>
 * <p>The {@code /stream} endpoint establishes a Server-Sent Events connection
 * for receiving real-time notifications. The connection stays open until
 * the client disconnects or the timeout is reached.</p>
 *
 * <h4>Authentication:</h4>
 * <p>All endpoints require authentication. The authenticated user is
 * automatically resolved and can only access their own notifications.</p>
 *
 * @see NotificationService
 * @see SseEmitterManager
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management and real-time streaming")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterManager sseEmitterManager;
    private final SseConfig sseConfig;

    // =========================================================================
    // SSE Stream
    // =========================================================================

    /**
     * Establishes an SSE connection for real-time notifications.
     *
     * <p>The client should call this endpoint when the application loads
     * and maintain the connection to receive push notifications.</p>
     *
     * <h4>Client Usage:</h4>
     * <pre>{@code
     * const eventSource = new EventSource('/api/notifications/stream');
     *
     * eventSource.addEventListener('notification', (event) => {
     *     const notification = JSON.parse(event.data);
     *     showToast(notification);
     * });
     *
     * eventSource.addEventListener('heartbeat', () => {
     *     // Connection is alive
     * });
     *
     * eventSource.onerror = () => {
     *     // Handle reconnection
     * };
     * }</pre>
     *
     * @param user the authenticated user
     * @return SSE emitter for the connection
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        log.info("🔗 [SSE] Stream requested by user: {}", user.getPublicId());

        // Create emitter with configured timeout
        long timeout = sseConfig.getTimeout();
        log.info("🔗 [SSE] Creating emitter with timeout: {}ms ({}min)",
                timeout, timeout / 60000);

        SseEmitter emitter = new SseEmitter(timeout);

        // Register the emitter
        log.info("🔗 [SSE] Registering emitter for user: {}", user.getPublicId());
        sseEmitterManager.register(user.getPublicId(), emitter);

        // Vérifier immédiatement que l'enregistrement a fonctionné
        boolean isRegistered = sseEmitterManager.isConnected(user.getPublicId());
        log.info("🔗 [SSE] Post-register check: isConnected={}, connectionCount={}",
                isRegistered, sseEmitterManager.getConnectionCount());

        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "status", "connected",
                            "userId", user.getPublicId()
                    )));
            log.info("✅ [SSE] Connection confirmation sent to user: {}", user.getPublicId());

            // Re-vérifier après l'envoi
            boolean stillRegistered = sseEmitterManager.isConnected(user.getPublicId());
            log.info("🔗 [SSE] Post-send check: isConnected={}", stillRegistered);

        } catch (Exception e) {
            log.error("❌ [SSE] Failed to send connection confirmation to user {}: {}",
                    user.getPublicId(), e.getMessage(), e);
        }

        return emitter;
    }

    // =========================================================================
    // List & Query
    // =========================================================================

    /**
     * Gets all notifications for the authenticated user.
     *
     * @param user     the authenticated user
     * @param pageable pagination parameters
     * @return page of notifications
     */
    @GetMapping
    @Operation(summary = "Get all notifications", description = "Returns paginated list of user's notifications")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<Notification> notifications = notificationService.getUserNotifications(user, pageable);
        Page<NotificationResponse> response = notifications.map(NotificationResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets active (non-dismissed) notifications for the authenticated user.
     *
     * @param user     the authenticated user
     * @param pageable pagination parameters
     * @return page of active notifications
     */
    @GetMapping("/active")
    @Operation(summary = "Get active notifications", description = "Returns non-dismissed, non-expired notifications")
    public ResponseEntity<Page<NotificationResponse>> getActiveNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Notification> notifications = notificationService.getActiveNotifications(user, pageable);
        Page<NotificationResponse> response = notifications.map(NotificationResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets unread notifications for the authenticated user.
     *
     * @param user the authenticated user
     * @return list of unread notifications
     */
    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Returns all unread notifications")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal User user) {

        List<Notification> notifications = notificationService.getUnreadNotifications(user);
        List<NotificationResponse> response = NotificationResponse.from(notifications);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets the count of unread notifications.
     *
     * @param user the authenticated user
     * @return unread count
     */
    @GetMapping("/unread/count")
    @Operation(summary = "Get unread count", description = "Returns the number of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.countUnread(user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Gets a single notification by public ID.
     *
     * @param user     the authenticated user
     * @param publicId the notification's public ID
     * @return the notification
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get notification by ID", description = "Returns a single notification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification found"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> getNotification(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Notification public ID") @PathVariable String publicId) {

        Notification notification = notificationService.getByPublicIdAndUser(publicId, user);
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    // =========================================================================
    // Status Updates
    // =========================================================================

    /**
     * Marks a notification as read.
     *
     * @param user     the authenticated user
     * @param publicId the notification's public ID
     * @return the updated notification
     */
    @PatchMapping("/{publicId}/read")
    @Operation(summary = "Mark as read", description = "Marks a notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {

        Notification notification = notificationService.markAsRead(publicId, user);
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    /**
     * Marks all notifications as read.
     *
     * @param user the authenticated user
     * @return count of updated notifications
     */
    @PostMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Marks all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(@AuthenticationPrincipal User user) {
        int count = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("updated", count));
    }

    /**
     * Dismisses a notification.
     *
     * @param user     the authenticated user
     * @param publicId the notification's public ID
     * @return the updated notification
     */
    @PatchMapping("/{publicId}/dismiss")
    @Operation(summary = "Dismiss notification", description = "Dismisses a notification (hides from list)")
    public ResponseEntity<NotificationResponse> dismiss(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {

        Notification notification = notificationService.dismiss(publicId, user);
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    /**
     * Dismisses all notifications.
     *
     * @param user the authenticated user
     * @return count of dismissed notifications
     */
    @PostMapping("/dismiss-all")
    @Operation(summary = "Dismiss all", description = "Dismisses all notifications")
    public ResponseEntity<Map<String, Integer>> dismissAll(@AuthenticationPrincipal User user) {
        int count = notificationService.dismissAll(user);
        return ResponseEntity.ok(Map.of("dismissed", count));
    }

    // =========================================================================
    // Delete
    // =========================================================================

    /**
     * Deletes a notification permanently.
     *
     * @param user     the authenticated user
     * @param publicId the notification's public ID
     * @return no content
     */
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete notification", description = "Permanently deletes a notification")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {

        notificationService.delete(publicId, user);
        return ResponseEntity.noContent().build();
    }
}