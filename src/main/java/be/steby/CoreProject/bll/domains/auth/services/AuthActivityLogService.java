package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.activitylog.AbstractActivityLogService;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.AuthAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Activity logging service for authentication domain
 * Handles logging of login, logout, security events, etc.
 */
@Service
@Slf4j
public class AuthActivityLogService extends AbstractActivityLogService {

    public AuthActivityLogService(ActivityLogRepository activityLogRepository,
                                  ApplicationEventPublisher eventPublisher) {
        super(activityLogRepository, eventPublisher);
    }

    @Override
    protected String getDomainName() {
        return "AUTH";
    }

    // ================== LOGIN RELATED METHODS ==================

    /**
     * Log successful user login
     */
    public void logSuccessfulLogin(User user, Device device, RequestContext context) {
        validateRequiredParams(user, AuthAction.LOGIN);

        String details = String.format("Successful login for user %s", user.getUsername());
        publishActionEvent(user, device, AuthAction.LOGIN, true, details, context);
    }

    /**
     * Log failed login attempt
     */
    public void logFailedLogin(User user, Device device, String failureReason, RequestContext context) {
        validateRequiredParams(user, AuthAction.LOGIN_FAILED);

        String details = String.format("Failed login attempt for user %s", user.getUsername());
        publishFailureEvent(user, device, AuthAction.LOGIN_FAILED, details, failureReason, context);
    }

    /**
     * Log failed login attempt with email (when user is not found)
     */
    public void logFailedLoginByEmail(String email, Device device, String failureReason, RequestContext context) {
        // For security reasons, we still need a User entity to log
        // This method might be called from a service that creates a temporary user record
        // or you might need to handle this differently based on your security requirements
        String details = String.format("Failed login attempt for email %s", email);

        // Note: This might need adjustment based on your security policy
        // You might want to log this differently or not at all for security reasons
        log.warn("Failed login attempt for email: {} - Reason: {}", email, failureReason);
    }

    /**
     * Log user logout
     */
    public void logLogout(User user, Device device, RequestContext context) {
        validateRequiredParams(user, AuthAction.LOGOUT);

        String details = String.format("User %s logged out", user.getUsername());
        publishActionEvent(user, device, AuthAction.LOGOUT, true, details, context);
    }

    // ================== SECURITY RELATED METHODS ==================

    /**
     * Log account locked due to security
     */
    public void logAccountLocked(User user, Device device, String reason, RequestContext context) {
        validateRequiredParams(user, AuthAction.ACCOUNT_LOCKED);

        String details = String.format("Account locked for user %s", user.getUsername());
        publishActionEvent(user, device, AuthAction.ACCOUNT_LOCKED, true, details, context);
    }

    /**
     * Log session expired
     */
    public void logSessionExpired(User user, Device device, RequestContext context) {
        validateRequiredParams(user, AuthAction.SESSION_EXPIRED);

        String details = String.format("Session expired for user %s", user.getUsername());
        publishActionEvent(user, device, AuthAction.SESSION_EXPIRED, true, details, context);
    }

    // ================== TWO-FACTOR AUTHENTICATION METHODS ==================

    /**
     * Log successful two-factor authentication
     */
    public void logTwoFactorSuccess(User user, Device device, String method, RequestContext context) {
        validateRequiredParams(user, AuthAction.TWO_FACTOR_SUCCESS);

        String details = String.format("Two-factor authentication successful for user %s using %s",
                user.getUsername(), method);
        publishActionEvent(user, device, AuthAction.TWO_FACTOR_SUCCESS, true, details, context);
    }

    /**
     * Log failed two-factor authentication
     */
    public void logTwoFactorFailed(User user, Device device, String method, String failureReason, RequestContext context) {
        validateRequiredParams(user, AuthAction.TWO_FACTOR_FAILED);

        String details = String.format("Two-factor authentication failed for user %s using %s",
                user.getUsername(), method);
        publishFailureEvent(user, device, AuthAction.TWO_FACTOR_FAILED, details, failureReason, context);
    }

    // ================== SECURITY QUESTIONS METHODS ==================

    /**
     * Log security question answered
     */
    public void logSecurityQuestionAnswered(User user, Device device, boolean correct, RequestContext context) {
        validateRequiredParams(user, AuthAction.SECURITY_QUESTION_ANSWERED);

        String details = String.format("Security question %s for user %s",
                correct ? "correctly answered" : "incorrectly answered", user.getUsername());

        if (correct) {
            publishActionEvent(user, device, AuthAction.SECURITY_QUESTION_ANSWERED, true, details, context);
        } else {
            publishFailureEvent(user, device, AuthAction.SECURITY_QUESTION_ANSWERED, details,
                    "Incorrect answer", context);
        }
    }

    // ================== REMEMBER ME TOKEN METHODS ==================

    /**
     * Log remember me token created
     */
    public void logRememberMeTokenCreated(User user, Device device, RequestContext context) {
        validateRequiredParams(user, AuthAction.REMEMBER_ME_TOKEN_CREATED);

        String details = String.format("Remember me token created for user %s", user.getUsername());
        publishActionEvent(user, device, AuthAction.REMEMBER_ME_TOKEN_CREATED, true, details, context);
    }

    /**
     * Log remember me token used
     */
    public void logRememberMeTokenUsed(User user, Device device, boolean valid, RequestContext context) {
        validateRequiredParams(user, AuthAction.REMEMBER_ME_TOKEN_USED);

        String details = String.format("Remember me token %s for user %s",
                valid ? "successfully used" : "invalid/expired", user.getUsername());

        if (valid) {
            publishActionEvent(user, device, AuthAction.REMEMBER_ME_TOKEN_USED, true, details, context);
        } else {
            publishFailureEvent(user, device, AuthAction.REMEMBER_ME_TOKEN_USED, details,
                    "Token invalid or expired", context);
        }
    }

    // ================== ANALYSIS METHODS ==================

    /**
     * Get recent login attempts for a user (successful and failed)
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentLoginAttempts(User user, int limit) {
        // This would need a custom query method - simplified for now
        return activityLogRepository.findTop10ByUserAndActionTypeOrderByTimestampDesc(
                user, AuthAction.LOGIN.getName()
        );
    }

    /**
     * Get recent failed login attempts
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentFailedLoginAttempts(User user, int limit) {
        return activityLogRepository.findTop10ByUserAndActionTypeOrderByTimestampDesc(
                user, AuthAction.LOGIN_FAILED.getName()
        );
    }

    /**
     * Check if user has had too many failed login attempts recently
     */
    public boolean hasTooManyFailedLoginAttempts(User user, int maxAttempts, int timeWindowMinutes) {
        // Convert minutes to days for the countActionType method
        // Note: for short time windows, we use direct repository call for precision
        if (timeWindowMinutes < 60) {
            Instant since = Instant.now().minusSeconds(timeWindowMinutes * 60L);
            long failedAttempts = activityLogRepository.countByUserAndActionTypeAndTimestampAfter(
                    user, AuthAction.LOGIN_FAILED.getName(), since
            );
            return failedAttempts >= maxAttempts;
        } else {
            // For longer periods, use the inherited method
            long failedAttempts = countActionType(user, AuthAction.LOGIN_FAILED, timeWindowMinutes / (24 * 60));
            return failedAttempts >= maxAttempts;
        }
    }

    /**
     * Check if user has recent successful login (for session validation)
     */
    public boolean hasRecentSuccessfulLogin(User user, int minutesThreshold) {
        return hasRecentAction(user, AuthAction.LOGIN, minutesThreshold);
    }

    /**
     * Get login statistics for user (last 30 days)
     */
    public AuthLoginStats getLoginStats(User user) {
        long successfulLogins = countActionType(user, AuthAction.LOGIN, 30);
        long failedLogins = countActionType(user, AuthAction.LOGIN_FAILED, 30);

        return new AuthLoginStats(successfulLogins, failedLogins);
    }

    /**
     * Find suspicious authentication activities
     * (e.g., multiple login attempts from different devices in short time)
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> findSuspiciousAuthActivities(User user, int hoursWindow) {
        Instant startTime = Instant.now().minusSeconds(hoursWindow * 3600L);
        Instant endTime = Instant.now();

        return activityLogRepository.findSuspiciousAuthActivities(user, startTime, endTime);
    }

    // ================== HELPER CLASSES ==================

    /**
     * Simple data class for login statistics
     */
    public record AuthLoginStats(long successfulLogins, long failedLogins) {
        public long totalAttempts() {
            return successfulLogins + failedLogins;
        }

        public double successRate() {
            if (totalAttempts() == 0) return 0.0;
            return (double) successfulLogins / totalAttempts();
        }
    }
}