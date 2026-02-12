package be.steby.CoreProject.bll.domains.notification.services;

import be.steby.CoreProject.bll.domains.notification.models.NotificationRequest;
import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for notification management and delivery.
 *
 * <p>This is the primary entry point for creating and sending notifications
 * in the system. It orchestrates the notification lifecycle including
 * channel selection, preference checking, and delivery dispatch.</p>
 *
 * <h4>Responsibilities:</h4>
 * <ul>
 *   <li><b>Send notifications:</b> Create and dispatch through appropriate channels</li>
 *   <li><b>Respect preferences:</b> Check user preferences before delivery</li>
 *   <li><b>Manage lifecycle:</b> Handle read/dismiss/delete operations</li>
 *   <li><b>Query notifications:</b> Retrieve user's notifications</li>
 * </ul>
 *
 * <h4>Channel Selection Logic:</h4>
 * <p>When sending a notification, channels are determined by:</p>
 * <ol>
 *   <li>If explicit channels specified in request → use those</li>
 *   <li>Otherwise, get user preferences for the notification type</li>
 *   <li>Filter by enabled channels and quiet hours</li>
 *   <li>For URGENT priority → may bypass quiet hours</li>
 * </ol>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Send a simple notification
 * NotificationRequest request = NotificationRequest.builder()
 *     .recipient(user)
 *     .type(NotificationType.REMINDER)
 *     .title("Meeting in 15 minutes")
 *     .body("Team standup in Conference Room A")
 *     .actionUrl("/calendar/events/abc123")
 *     .build();
 *
 * Notification notification = notificationService.send(request);
 *
 * // Get user's unread count
 * long unreadCount = notificationService.countUnread(user);
 * }</pre>
 *
 * @see NotificationRequest
 * @see Notification
 */
public interface NotificationService {

    // =========================================================================
    // Send Operations
    // =========================================================================

    /**
     * Sends a notification to a user.
     *
     * <p>This is the primary method for creating and dispatching notifications.
     * The notification will be persisted and delivered through the appropriate
     * channels based on user preferences and notification type.</p>
     *
     * <p><b>Flow:</b></p>
     * <ol>
     *   <li>Create notification entity from request</li>
     *   <li>Determine delivery channels (explicit or from preferences)</li>
     *   <li>Check quiet hours (may delay non-urgent notifications)</li>
     *   <li>Dispatch to each enabled channel</li>
     *   <li>Update notification status</li>
     * </ol>
     *
     * @param request the notification request
     * @return the created and dispatched notification
     * @throws IllegalArgumentException if request validation fails
     */
    Notification send(NotificationRequest request);

    /**
     * Sends notifications to multiple users.
     *
     * <p>Batch operation for sending the same notification to multiple recipients.
     * Each user's preferences are respected individually.</p>
     *
     * @param recipients the users to notify
     * @param type       the notification type
     * @param title      the notification title
     * @param body       the notification body
     * @return list of created notifications
     */
    List<Notification> sendToMany(
            List<User> recipients,
            NotificationType type,
            String title,
            String body
    );

    /**
     * Sends notifications to multiple users with action URL.
     *
     * @param recipients the users to notify
     * @param type       the notification type
     * @param title      the notification title
     * @param body       the notification body
     * @param actionUrl  the action URL
     * @return list of created notifications
     */
    List<Notification> sendToMany(
            List<User> recipients,
            NotificationType type,
            String title,
            String body,
            String actionUrl
    );

    // =========================================================================
    // Query Operations
    // =========================================================================

    /**
     * Retrieves a notification by its public ID.
     *
     * @param publicId the notification's public UUID
     * @return the notification
     * @throws be.steby.CoreProject.bll.domains.notification.exceptions.NotificationNotFoundException
     *         if notification not found
     */
    Notification getByPublicId(String publicId);

    /**
     * Retrieves a notification by public ID, verifying ownership.
     *
     * @param publicId the notification's public UUID
     * @param user     the expected recipient
     * @return the notification
     * @throws be.steby.CoreProject.bll.domains.notification.exceptions.NotificationNotFoundException
     *         if notification not found or not owned by user
     */
    Notification getByPublicIdAndUser(String publicId, User user);

    /**
     * Retrieves all notifications for a user.
     *
     * @param user     the notification recipient
     * @param pageable pagination parameters
     * @return page of notifications
     */
    Page<Notification> getUserNotifications(User user, Pageable pageable);

    /**
     * Retrieves active (non-dismissed, non-expired) notifications for a user.
     *
     * @param user     the notification recipient
     * @param pageable pagination parameters
     * @return page of active notifications
     */
    Page<Notification> getActiveNotifications(User user, Pageable pageable);

    /**
     * Retrieves unread notifications for a user.
     *
     * @param user the notification recipient
     * @return list of unread notifications
     */
    List<Notification> getUnreadNotifications(User user);

    /**
     * Counts unread notifications for a user.
     *
     * <p>Used for displaying the notification badge count.</p>
     *
     * @param user the notification recipient
     * @return count of unread notifications
     */
    long countUnread(User user);

    /**
     * Checks if user has any unread notifications.
     *
     * @param user the notification recipient
     * @return true if there are unread notifications
     */
    boolean hasUnread(User user);

    // =========================================================================
    // Status Update Operations
    // =========================================================================

    /**
     * Marks a notification as read.
     *
     * @param publicId the notification's public UUID
     * @param user     the notification recipient
     * @return the updated notification
     */
    Notification markAsRead(String publicId, User user);

    /**
     * Marks multiple notifications as read.
     *
     * @param publicIds the notification public UUIDs
     * @param user      the notification recipient
     * @return number of notifications marked as read
     */
    int markAsRead(List<String> publicIds, User user);

    /**
     * Marks all notifications as read for a user.
     *
     * @param user the notification recipient
     * @return number of notifications marked as read
     */
    int markAllAsRead(User user);

    /**
     * Dismisses a notification.
     *
     * <p>Dismissed notifications are hidden from the notification center
     * but not deleted from the database.</p>
     *
     * @param publicId the notification's public UUID
     * @param user     the notification recipient
     * @return the updated notification
     */
    Notification dismiss(String publicId, User user);

    /**
     * Dismisses all notifications for a user.
     *
     * @param user the notification recipient
     * @return number of notifications dismissed
     */
    int dismissAll(User user);

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Deletes a notification permanently.
     *
     * @param publicId the notification's public UUID
     * @param user     the notification recipient
     */
    void delete(String publicId, User user);

    /**
     * Deletes notifications by source reference.
     *
     * <p>Used when the source entity is deleted (e.g., calendar event cancelled).
     * Removes all notifications linked to that source.</p>
     *
     * @param sourceDomain      the source domain (e.g., "calendar")
     * @param sourceReferenceId the source entity's public ID
     * @return number of notifications deleted
     */
    int deleteBySource(String sourceDomain, String sourceReferenceId);

    // =========================================================================
    // Scheduled Notifications
    // =========================================================================

    /**
     * Processes scheduled notifications that are ready for delivery.
     *
     * <p>Called by the scheduler to deliver notifications whose
     * scheduled time has arrived.</p>
     *
     * @return number of notifications processed
     */
    int processScheduledNotifications();

    /**
     * Cancels a scheduled notification.
     *
     * @param publicId the notification's public UUID
     * @param user     the notification recipient
     * @return true if notification was cancelled, false if already sent
     */
    boolean cancelScheduled(String publicId, User user);
}