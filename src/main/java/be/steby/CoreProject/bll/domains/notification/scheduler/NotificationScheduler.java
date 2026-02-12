package be.steby.CoreProject.bll.domains.notification.scheduler;

import be.steby.CoreProject.bll.domains.notification.services.NotificationService;
import be.steby.CoreProject.dal.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduler for notification-related background tasks.
 *
 * <p>Handles periodic tasks such as:</p>
 * <ul>
 *   <li>Processing scheduled notifications (reminders)</li>
 *   <li>Cleaning up expired/old notifications</li>
 * </ul>
 *
 * <h4>Configuration:</h4>
 * <pre>{@code
 * app:
 *   notification:
 *     scheduler:
 *       enabled: true
 *       process-interval: 60000      # 1 minute
 *       cleanup-interval: 86400000   # 24 hours
 *       retention-days: 90           # Keep dismissed for 90 days
 * }</pre>
 *
 * <h4>Scheduled Tasks:</h4>
 * <ul>
 *   <li><b>processScheduledNotifications:</b> Runs every minute, sends
 *       notifications whose scheduled time has arrived</li>
 *   <li><b>cleanupOldNotifications:</b> Runs daily, removes old dismissed
 *       notifications and expired pending notifications</li>
 * </ul>
 *
 * @see NotificationService
 */
@Component
@ConditionalOnProperty(name = "app.notification.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    /**
     * Default retention period for dismissed notifications (days).
     */
    private static final int DEFAULT_RETENTION_DAYS = 90;

    // =========================================================================
    // Scheduled Notification Processing
    // =========================================================================

    /**
     * Processes scheduled notifications that are ready for delivery.
     *
     * <p>Runs every minute (configurable via property). Finds all notifications
     * with {@code scheduledFor} in the past and {@code status = PENDING},
     * then dispatches them through the normal delivery flow.</p>
     *
     * <p>This enables features like:</p>
     * <ul>
     *   <li>Calendar event reminders (15 min before, 1 hour before, etc.)</li>
     *   <li>Scheduled announcements</li>
     *   <li>Delayed notifications</li>
     * </ul>
     */
    @Scheduled(fixedRateString = "${app.notification.scheduler.process-interval:60000}")
    public void processScheduledNotifications() {
        log.trace("Running scheduled notification processor");

        try {
            int processed = notificationService.processScheduledNotifications();

            if (processed > 0) {
                log.info("Processed {} scheduled notification(s)", processed);
            }
        } catch (Exception e) {
            log.error("Error processing scheduled notifications: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Cleanup Tasks
    // =========================================================================

    /**
     * Cleans up old and expired notifications.
     *
     * <p>Runs daily at 3:00 AM (configurable). Performs:</p>
     * <ul>
     *   <li>Delete expired pending notifications (never delivered)</li>
     *   <li>Delete old dismissed notifications (beyond retention period)</li>
     * </ul>
     *
     * <p>This prevents the notification table from growing indefinitely
     * while preserving recent history for user reference.</p>
     */
    @Scheduled(cron = "${app.notification.scheduler.cleanup-cron:0 0 3 * * ?}")
    public void cleanupOldNotifications() {
        log.info("Running notification cleanup task");

        try {
            // Delete expired pending notifications
            Instant expiredBefore = Instant.now();
            int expiredDeleted = notificationRepository.deleteExpiredPending(expiredBefore);

            if (expiredDeleted > 0) {
                log.info("Deleted {} expired pending notification(s)", expiredDeleted);
            }

            // Delete old dismissed notifications
            Instant dismissedBefore = Instant.now().minus(DEFAULT_RETENTION_DAYS, ChronoUnit.DAYS);
            int oldDeleted = notificationRepository.deleteOldDismissed(dismissedBefore);

            if (oldDeleted > 0) {
                log.info("Deleted {} old dismissed notification(s)", oldDeleted);
            }

            log.info("Notification cleanup completed: {} expired, {} old dismissed",
                    expiredDeleted, oldDeleted);

        } catch (Exception e) {
            log.error("Error during notification cleanup: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Manual Triggers (for admin/testing)
    // =========================================================================

    /**
     * Manually triggers scheduled notification processing.
     *
     * <p>Can be called from an admin endpoint for testing or
     * to force immediate processing.</p>
     *
     * @return number of notifications processed
     */
    public int triggerProcessScheduled() {
        log.info("Manual trigger: processing scheduled notifications");
        return notificationService.processScheduledNotifications();
    }

    /**
     * Manually triggers cleanup task.
     *
     * <p>Can be called from an admin endpoint for maintenance.</p>
     */
    public void triggerCleanup() {
        log.info("Manual trigger: running notification cleanup");
        cleanupOldNotifications();
    }
}