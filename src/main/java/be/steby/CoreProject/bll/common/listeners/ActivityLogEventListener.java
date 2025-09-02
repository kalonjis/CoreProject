package be.steby.CoreProject.bll.common.listeners;

import be.steby.CoreProject.bll.common.events.UserActionEvent;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Global listener that handles ALL user action events asynchronously
 * This single listener processes events from all domains: AUTH, PASSWORD, EMAIL, etc.
 * Uses dedicated activityLogExecutor thread pool for optimal performance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogEventListener {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Handle all user action events asynchronously using dedicated activity log executor
     * This method processes events from all activity log domains
     */
    @EventListener
    @Async("activityLogExecutor")  // ✅ CORRECTION: Utilise le thread pool dédié
    public void handleUserActionEvent(UserActionEvent event) {
        try {
            // Create ActivityLog using the factory method from ActionLogType
            ActivityLog activityLog = event.actionType()
                    .createActivityLog(event.user(), event.successful());

            // Enrich with device information
            if (event.device() != null) {
                activityLog.setDevice(event.device());
            }

            // Add details if provided
            if (event.details() != null && !event.details().trim().isEmpty()) {
                activityLog.setDetails(event.details().trim());
            }

            // Add failure reason for failed actions
            if (!event.successful() && event.failureReason() != null && !event.failureReason().trim().isEmpty()) {
                activityLog.setFailureReason(event.failureReason().trim());
            }

            // Save to database
            ActivityLog savedLog = activityLogRepository.save(activityLog);

            // Log success (debug level to avoid spam)
            log.debug("Activity logged successfully: {} for user {} - {} (ID: {})",
                    event.actionType(),
                    event.user().getUsername(),
                    event.successful() ? "SUCCESS" : "FAILED",
                    savedLog.getId());

            // Log failure details if present
            if (!event.successful() && event.failureReason() != null) {
                log.debug("Failure reason: {}", event.failureReason());
            }

        } catch (Exception e) {
            // Log error but don't rethrow - activity logging should never break business logic
            log.error("Failed to log activity {} for user {}: {}",
                    event.actionType(),
                    event.user().getUsername(),
                    e.getMessage(), e);
        }
    }

    /**
     * Handles high-priority security events with additional logging
     * These events might need immediate attention
     */
    @EventListener
    @Async("activityLogExecutor")  // ✅ CORRECTION: Utilise le thread pool dédié
    public void handleSecurityEvent(UserActionEvent event) {
        // Only process security-related events
        if (!isSecurityRelatedEvent(event)) {
            return;
        }

        try {
            // Process normal logging first
            handleUserActionEvent(event);

            // Additional security logging
            if (event.isFailure()) {
                log.warn("Security event failed: {} for user {} from device {} - Reason: {}",
                        event.actionType(),
                        event.user().getUsername(),
                        event.device() != null ? event.device().getId() : "unknown",
                        event.failureReason());
            } else {
                log.info("Security event succeeded: {} for user {} from device {}",
                        event.actionType(),
                        event.user().getUsername(),
                        event.device() != null ? event.device().getId() : "unknown");
            }

        } catch (Exception e) {
            log.error("Failed to process security event {} for user {}: {}",
                    event.actionType(), event.user().getUsername(), e.getMessage(), e);
        }
    }

    // ================== PRIVATE HELPER METHODS ==================

    /**
     * Determine if an event is security-related and needs special attention
     */
    private boolean isSecurityRelatedEvent(UserActionEvent event) {
        String actionType = event.actionType().getName();

        return actionType.contains("LOGIN_FAILED") ||
                actionType.contains("ACCOUNT_LOCKED") ||
                actionType.contains("TWO_FACTOR_FAILED") ||
                actionType.contains("SECURITY") ||
                actionType.contains("SUSPICIOUS") ||
                (event.getActionCategory().equals("AUTH") && event.isFailure());
    }
}