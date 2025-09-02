package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.activitylog.AbstractActivityLogService;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.PasswordAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Activity logging service for password domain
 * Handles logging of password changes, resets, policy violations, etc.
 */
@Service
@Slf4j
public class PasswordActivityLogService extends AbstractActivityLogService {

    public PasswordActivityLogService(ActivityLogRepository activityLogRepository,
                                      ApplicationEventPublisher eventPublisher) {
        super(activityLogRepository, eventPublisher);
    }

    @Override
    protected String getDomainName() {
        return "PASSWORD";
    }

    // ================== PASSWORD CHANGE METHODS ==================

    /**
     * Log successful password change by authenticated user
     */
    public void logPasswordChanged(User user, Device device, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_CHANGED);

        String details = String.format("Password changed successfully for user %s", user.getUsername());
        publishActionEvent(user, device, PasswordAction.PASSWORD_CHANGED, true, details, context);
    }

    /**
     * Log failed password change attempt
     */
    public void logPasswordChangeFailed(User user, Device device, String failureReason, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_CHANGED_FAILED);

        String details = String.format("Password change failed for user %s", user.getUsername());
        publishFailureEvent(user, device, PasswordAction.PASSWORD_CHANGED_FAILED, details, failureReason, context);
    }

    /**
     * Log current password verification failure
     */
    public void logCurrentPasswordIncorrect(User user, Device device, RequestContext context) {
        validateRequiredParams(user, PasswordAction.CURRENT_PASSWORD_INCORRECT);

        String details = String.format("Current password verification failed for user %s", user.getUsername());
        publishFailureEvent(user, device, PasswordAction.CURRENT_PASSWORD_INCORRECT, details,
                "Current password is incorrect", context);
    }

    // ================== PASSWORD RESET METHODS ==================

    /**
     * Log successful password reset via token
     */
    public void logPasswordReset(User user, Device device, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_RESET);

        String details = String.format("Password reset successfully for user %s", user.getUsername());
        publishActionEvent(user, device, PasswordAction.PASSWORD_RESET, true, details, context);
    }

    /**
     * Log failed password reset attempt
     */
    public void logPasswordResetFailed(User user, Device device, String failureReason, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_RESET_FAILED);

        String details = String.format("Password reset failed for user %s", user.getUsername());
        publishFailureEvent(user, device, PasswordAction.PASSWORD_RESET_FAILED, details, failureReason, context);
    }

    /**
     * Log password reset request (email sent)
     */
    public void logPasswordResetRequested(User user, Device device, String email, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_RESET_REQUESTED);

        String details = String.format("Password reset requested for email %s", email);
        publishActionEvent(user, device, PasswordAction.PASSWORD_RESET_REQUESTED, true, details, context);
    }

    /**
     * Log password reset request by email (when user is not found)
     */
    public void logPasswordResetRequestedByEmail(String email, Device device, RequestContext context) {
        // For security reasons, we log this but with minimal information
        log.info("Password reset requested for email: {} - User lookup performed", email);
        // Note: We don't create activity log entry for non-existent users for security reasons
    }

    /**
     * Log new password reset token request
     */
    public void logPasswordResetTokenRequested(User user, Device device, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_RESET_TOKEN_REQUESTED);

        String details = String.format("New password reset token requested for user %s", user.getUsername());
        publishActionEvent(user, device, PasswordAction.PASSWORD_RESET_TOKEN_REQUESTED, true, details, context);
    }

    // ================== SECURITY AND POLICY METHODS ==================

    /**
     * Log password policy violation
     */
    public void logPasswordPolicyViolation(User user, Device device, List<String> violations, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_POLICY_VIOLATION);

        String details = String.format("Password policy violations for user %s: %s",
                user.getUsername(), String.join(", ", violations));
        publishFailureEvent(user, device, PasswordAction.PASSWORD_POLICY_VIOLATION, details,
                "Password does not meet security requirements", context);
    }

    /**
     * Log password expiration event
     */
    public void logPasswordExpired(User user, Device device, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_EXPIRED);

        String details = String.format("Password expired for user %s - change required", user.getUsername());
        publishActionEvent(user, device, PasswordAction.PASSWORD_EXPIRED, true, details, context);
    }

    /**
     * Log administrative password force change
     */
    public void logPasswordForceChanged(User user, Device device, String adminUsername, RequestContext context) {
        validateRequiredParams(user, PasswordAction.PASSWORD_FORCE_CHANGED);

        String details = String.format("Password forcibly changed for user %s by administrator %s",
                user.getUsername(), adminUsername);
        publishActionEvent(user, device, PasswordAction.PASSWORD_FORCE_CHANGED, true, details, context);
    }

    // ================== ANALYSIS METHODS ==================

    /**
     * Get recent password changes for a user
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentPasswordChanges(User user, int limit) {
        return activityLogRepository.findTop10ByUserAndActionTypeOrderByTimestampDesc(
                user, PasswordAction.PASSWORD_CHANGED.getName()
        );
    }

    /**
     * Get recent failed password attempts
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentFailedPasswordAttempts(User user, int limit) {
        return activityLogRepository.findTop10ByUserAndActionTypeOrderByTimestampDesc(
                user, PasswordAction.PASSWORD_CHANGED_FAILED.getName()
        );
    }

    /**
     * Check if user has changed password recently
     */
    public boolean hasRecentPasswordChange(User user, int daysThreshold) {
        return hasRecentAction(user, PasswordAction.PASSWORD_CHANGED, daysThreshold);
    }

    /**
     * Check if user has too many failed password change attempts
     */
    public boolean hasTooManyFailedPasswordAttempts(User user, int maxAttempts, int timeWindowHours) {
        if (timeWindowHours < 24) {
            Instant since = Instant.now().minusSeconds(timeWindowHours * 3600L);
            long failedAttempts = activityLogRepository.countByUserAndActionTypeAndTimestampAfter(
                    user, PasswordAction.PASSWORD_CHANGED_FAILED.getName(), since
            );
            return failedAttempts >= maxAttempts;
        } else {
            // For longer periods, use the inherited method
            long failedAttempts = countActionType(user, PasswordAction.PASSWORD_CHANGED_FAILED, timeWindowHours / 24);
            return failedAttempts >= maxAttempts;
        }
    }

    /**
     * Get password activity statistics for user (last 30 days)
     */
    public PasswordStats getPasswordStats(User user) {
        long passwordChanges = countActionType(user, PasswordAction.PASSWORD_CHANGED, 30);
        long failedAttempts = countActionType(user, PasswordAction.PASSWORD_CHANGED_FAILED, 30);
        long resetRequests = countActionType(user, PasswordAction.PASSWORD_RESET_REQUESTED, 30);

        return new PasswordStats(passwordChanges, failedAttempts, resetRequests);
    }

    /**
     * Find suspicious password activities
     * (e.g., multiple failed attempts followed by successful change)
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> findSuspiciousPasswordActivities(User user, int hoursWindow) {
        Instant startTime = Instant.now().minusSeconds(hoursWindow * 3600L);
        Instant endTime = Instant.now();

        return activityLogRepository.findSuspiciousAuthActivities(user, startTime, endTime);
    }

    // ================== HELPER CLASSES ==================

    /**
     * Simple data class for password statistics
     */
    public record PasswordStats(long passwordChanges, long failedAttempts, long resetRequests) {
        public long totalActivity() {
            return passwordChanges + failedAttempts + resetRequests;
        }

        public double changeSuccessRate() {
            if (passwordChanges + failedAttempts == 0) return 0.0;
            return (double) passwordChanges / (passwordChanges + failedAttempts);
        }
    }
}