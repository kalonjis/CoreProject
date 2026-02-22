package be.steby.CoreProject.bll.domains.password.services.activitylog;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.admin.events.password.AdminPasswordResetTriggeredEvent;
import be.steby.CoreProject.bll.domains.password.events.*;
import be.steby.CoreProject.bll.domains.password.listeners.PasswordActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.PasswordAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Password domain activity log service.
 *
 * <p>Translates password domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to a {@link PasswordAction} constant.</p>
 *
 * <p>Called exclusively from {@link PasswordActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
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
@Service
@Slf4j
public class PasswordActivityLogService extends ActivityLogService {

    public PasswordActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "PASSWORD";
    }

    // =========================================================================
    // Password Change (authenticated user)
    // =========================================================================

    /**
     * Persists a {@link PasswordAction#PASSWORD_CHANGED} entry when a user
     * successfully changes their password while authenticated.
     *
     * @param event contains the user and device; user is never null
     */
    public void logPasswordChanged(PasswordChangedEvent event) {
        logUserActivity(event.user(), event.device(), PasswordAction.PASSWORD_CHANGED, true);
        log.debug("PASSWORD_CHANGED logged — user: {}", event.user().getUsername());
    }

    /**
     * Persists a {@link PasswordAction#PASSWORD_CHANGE_FAILED} entry when a user
     * attempts to change their password but provides an incorrect current password.
     *
     * <p>Important for brute-force detection on authenticated sessions.</p>
     *
     * @param event contains the user, device and failure reason; user is never null
     */
    public void logPasswordChangeFailed(PasswordChangeFailedEvent event) {
        logUserActivity(event.user(), event.device(), PasswordAction.PASSWORD_CHANGE_FAILED,
                false, event.failureReason());
        log.debug("PASSWORD_CHANGE_FAILED logged — user: {}, reason: {}",
                event.user().getUsername(), event.failureReason());
    }

    // =========================================================================
    // Password Reset (token-based recovery)
    // =========================================================================

    /**
     * Persists a {@link PasswordAction#PASSWORD_RESET_REQUESTED} entry when a user
     * initiates the password recovery flow.
     *
     * <p>May indicate account takeover attempt if not initiated by owner.</p>
     *
     * @param event contains the user and device; device may be null
     */
    public void logPasswordResetRequested(RequestPasswordResetEvent event) {
        logUserActivity(event.user(), event.device(), PasswordAction.PASSWORD_RESET_REQUESTED, true);
        log.debug("PASSWORD_RESET_REQUESTED logged — user: {}", event.user().getUsername());
    }

    /**
     * Persists a {@link PasswordAction#PASSWORD_RESET_TOKEN_REFRESHED} entry when a user
     * requests a new reset token because the previous one expired.
     *
     * <p>Multiple refreshes in short time may indicate delivery issues or attack.</p>
     *
     * @param event contains the user and device; device may be null
     */
    public void logPasswordResetTokenRefreshed(RequestPasswordTokenEvent event) {
        logUserActivity(event.user(), event.device(), PasswordAction.PASSWORD_RESET_TOKEN_REFRESHED, true);
        log.debug("PASSWORD_RESET_TOKEN_REFRESHED logged — user: {}", event.user().getUsername());
    }

    /**
     * Persists a {@link PasswordAction#PASSWORD_RESET_COMPLETED} entry when a user
     * successfully resets their password using a valid recovery token.
     *
     * @param event contains the user and device; device may be null
     */
    public void logPasswordResetCompleted(PasswordResetCompletedEvent event) {
        logUserActivity(event.user(), event.device(), PasswordAction.PASSWORD_RESET_COMPLETED, true);
        log.debug("PASSWORD_RESET_COMPLETED logged — user: {}", event.user().getUsername());
    }

    /**
     * Persists a {@link PasswordAction#PASSWORD_RESET_FAILED} entry when a user
     * attempts to reset their password with an invalid, expired, or used token.
     *
     * <p>High volume may indicate brute-force token guessing.</p>
     *
     * @param event contains the user (may be null), device and failure reason
     */
    public void logPasswordResetFailed(PasswordResetFailedEvent event) {
        logUserActivity(event.user(), event.device(), PasswordAction.PASSWORD_RESET_FAILED,
                false, event.failureReason());
        if (event.user() != null) {
            log.debug("PASSWORD_RESET_FAILED logged — user: {}, reason: {}",
                    event.user().getUsername(), event.failureReason());
        } else {
            log.debug("PASSWORD_RESET_FAILED logged — user: unknown, reason: {}",
                    event.failureReason());
        }
    }

    // =========================================================================
    // Admin-initiated
    // =========================================================================

    /**
     * Persists a {@link PasswordAction#PASSWORD_RESET_BY_ADMIN} entry when an
     * administrator forces a password reset for a user.
     *
     * <p>The action details include the admin username, strategy used, and reason.</p>
     *
     * @param event contains target user, admin user, strategy and reason
     */
    public void logPasswordResetByAdmin(AdminPasswordResetTriggeredEvent event) {
        String details = buildAdminResetDetails(event);
        logUserActivity(event.targetUser(), null, PasswordAction.PASSWORD_RESET_BY_ADMIN, true, details);
        log.debug("PASSWORD_RESET_BY_ADMIN logged — target: {}, admin: {}",
                event.targetUser().getUsername(), event.adminUser().getUsername());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Builds a compact detail string for admin-initiated password resets.
     */
    private String buildAdminResetDetails(AdminPasswordResetTriggeredEvent event) {
        StringBuilder details = new StringBuilder();
        details.append("by: ").append(event.adminUser().getUsername());
        details.append(" | strategy: ").append(event.strategy().name());
        if (event.hasReason()) {
            details.append(" | reason: ").append(event.reason());
        }
        return details.toString();
    }
}