package be.steby.CoreProject.bll.domains.password.listeners;

import be.steby.CoreProject.bll.domains.admin.events.password.AdminPasswordResetTriggeredEvent;
import be.steby.CoreProject.bll.domains.password.events.*;
import be.steby.CoreProject.bll.domains.password.services.activitylog.PasswordActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for password domain activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link PasswordActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after notification listeners
 * ({@code @Order(10)}) — a logging failure never blocks email/SMS delivery.</p>
 *
 * <h3>Covered events → PasswordAction mapping:</h3>
 * <ul>
 *   <li>{@link PasswordChangedEvent}            → PASSWORD_CHANGED</li>
 *   <li>{@link PasswordChangeFailedEvent}       → PASSWORD_CHANGE_FAILED</li>
 *   <li>{@link RequestPasswordResetEvent}       → PASSWORD_RESET_REQUESTED</li>
 *   <li>{@link RequestPasswordTokenEvent}       → PASSWORD_RESET_TOKEN_REFRESHED</li>
 *   <li>{@link PasswordResetCompletedEvent}     → PASSWORD_RESET_COMPLETED</li>
 *   <li>{@link PasswordResetFailedEvent}        → PASSWORD_RESET_FAILED</li>
 *   <li>{@link AdminPasswordResetTriggeredEvent} → PASSWORD_RESET_BY_ADMIN</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class PasswordActivityLogListener {

    private final PasswordActivityLogService passwordActivityLogService;

    // =========================================================================
    // Password Change (authenticated user)
    // =========================================================================

    /**
     * Handles successful password change events.
     *
     * @param event the password changed event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordChanged(PasswordChangedEvent event) {
        try {
            log.debug("Processing password changed event for user: {}", event.user().getUsername());
            passwordActivityLogService.logPasswordChanged(event);
        } catch (Exception e) {
            log.error("Failed to log PASSWORD_CHANGED for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Handles failed password change attempts.
     *
     * @param event the password change failed event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordChangeFailed(PasswordChangeFailedEvent event) {
        try {
            log.debug("Processing password change failed event for user: {}", event.user().getUsername());
            passwordActivityLogService.logPasswordChangeFailed(event);
        } catch (Exception e) {
            log.error("Failed to log PASSWORD_CHANGE_FAILED for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Password Reset (token-based recovery)
    // =========================================================================

    /**
     * Handles password reset request events.
     *
     * @param event the password reset requested event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordResetRequested(RequestPasswordResetEvent event) {
        try {
            log.debug("Processing password reset requested event for user: {}", event.user().getUsername());
            passwordActivityLogService.logPasswordResetRequested(event);
        } catch (Exception e) {
            log.error("Failed to log PASSWORD_RESET_REQUESTED for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Handles password reset token refresh events.
     *
     * @param event the token refresh event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordResetTokenRefreshed(RequestPasswordTokenEvent event) {
        try {
            log.debug("Processing password reset token refreshed event for user: {}", event.user().getUsername());
            passwordActivityLogService.logPasswordResetTokenRefreshed(event);
        } catch (Exception e) {
            log.error("Failed to log PASSWORD_RESET_TOKEN_REFRESHED for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Handles successful password reset completion events.
     *
     * @param event the password reset completed event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordResetCompleted(PasswordResetCompletedEvent event) {
        try {
            log.debug("Processing password reset completed event for user: {}", event.user().getUsername());
            passwordActivityLogService.logPasswordResetCompleted(event);
        } catch (Exception e) {
            log.error("Failed to log PASSWORD_RESET_COMPLETED for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Handles failed password reset attempts.
     *
     * @param event the password reset failed event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordResetFailed(PasswordResetFailedEvent event) {
        try {
            String username = event.user() != null ? event.user().getUsername() : "unknown";
            log.debug("Processing password reset failed event for user: {}", username);
            passwordActivityLogService.logPasswordResetFailed(event);
        } catch (Exception e) {
            log.error("Failed to log PASSWORD_RESET_FAILED: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Admin-initiated
    // =========================================================================

    /**
     * Handles admin-initiated password reset events.
     *
     * @param event the admin password reset triggered event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminPasswordReset(AdminPasswordResetTriggeredEvent event) {
        try {
            log.debug("Processing admin password reset event for target user: {}",
                    event.targetUser().getUsername());
            passwordActivityLogService.logPasswordResetByAdmin(event);
        } catch (Exception e) {
            log.error("Failed to log PASSWORD_RESET_BY_ADMIN for target user: {}",
                    event.targetUser().getUsername(), e);
        }
    }
}