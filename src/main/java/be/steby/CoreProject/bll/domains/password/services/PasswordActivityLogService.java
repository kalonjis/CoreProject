package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.PasswordAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password management domain activity logging service.
 * PURE logging service - records password events, nothing more.
 *
 * Principle: "We log password events, analysts analyze them"
 *
 * @author Steby Core Project Team
 * @version 2.0 - Pure KISS Edition
 */
@Service
@Slf4j
public class PasswordActivityLogService extends AbstractActivityLogService {

    // ================== CONSTRUCTOR ==================

    public PasswordActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "PASSWORD";
    }

    // ================== PASSWORD LIFECYCLE LOGGING ==================

    /**
     * Logs password creation
     */
    @Transactional
    public ActivityLog logPasswordCreated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_CREATED, context);
    }

    /**
     * Logs password update
     */
    @Transactional
    public ActivityLog logPasswordUpdated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_UPDATED, context);
    }

    /**
     * Logs password change by user
     */
    @Transactional
    public ActivityLog logPasswordChanged(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_CHANGED, context);
    }

    /**
     * Logs password expiration
     */
    @Transactional
    public ActivityLog logPasswordExpired(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_EXPIRED, context);
    }

    /**
     * Logs password force expiration by admin
     */
    @Transactional
    public ActivityLog logPasswordForceExpired(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, PasswordAction.PASSWORD_FORCE_EXPIRED, context);
        activityLog.setActionDetails("Force expired password for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    // ================== PASSWORD RESET PROCESS LOGGING ==================

    /**
     * Logs password reset request
     */
    @Transactional
    public ActivityLog logPasswordResetRequested(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_RESET_REQUESTED, context);
    }

    /**
     * Logs password reset completion
     */
    @Transactional
    public ActivityLog logPasswordResetCompleted(User user, boolean successful, String reason, Device device, RequestContext context) {
        if (successful) {
            return logSuccess(user, device, PasswordAction.PASSWORD_RESET_COMPLETED, context);
        } else {
            return logFailure(user, device, PasswordAction.PASSWORD_RESET_FAILED, reason, context);
        }
    }

    /**
     * Logs password reset token expiration
     */
    @Transactional
    public ActivityLog logPasswordResetExpired(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_RESET_EXPIRED, context);
    }

    /**
     * Logs password reset cancellation
     */
    @Transactional
    public ActivityLog logPasswordResetCancelled(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_RESET_CANCELLED, context);
    }

    /**
     * Logs temporary password issuance
     */
    @Transactional
    public ActivityLog logTemporaryPasswordIssued(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, PasswordAction.TEMPORARY_PASSWORD_ISSUED, context);
        activityLog.setActionDetails("Issued temporary password for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs temporary password usage
     */
    @Transactional
    public ActivityLog logTemporaryPasswordUsed(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.TEMPORARY_PASSWORD_USED, context);
    }

    // ================== PASSWORD VALIDATION LOGGING ==================

    /**
     * Logs password strength check
     */
    @Transactional
    public ActivityLog logPasswordStrengthChecked(User user, String strengthLevel, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, PasswordAction.PASSWORD_STRENGTH_CHECKED, context);
        activityLog.setActionDetails("Password strength: " + strengthLevel);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password history violation (reuse detected)
     */
    @Transactional
    public ActivityLog logPasswordHistoryViolation(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_HISTORY_VIOLATION, context);
    }

    /**
     * Logs password complexity failure
     */
    @Transactional
    public ActivityLog logPasswordComplexityFailed(User user, String reason, Device device, RequestContext context) {
        return logFailure(user, device, PasswordAction.PASSWORD_COMPLEXITY_FAILED, reason, context);
    }

    /**
     * Logs weak password detection
     */
    @Transactional
    public ActivityLog logPasswordTooWeak(User user, Device device, RequestContext context) {
        return logFailure(user, device, PasswordAction.PASSWORD_TOO_WEAK, "Password strength insufficient", context);
    }

    /**
     * Logs compromised password detection
     */
    @Transactional
    public ActivityLog logPasswordCompromisedDetected(User user, String source, Device device, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, PasswordAction.PASSWORD_COMPROMISED_DETECTED,
                "Password found in breach database", context);
        activityLog.setActionDetails("Detection source: " + source);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password breach database check
     */
    @Transactional
    public ActivityLog logPasswordBreachCheck(User user, boolean found, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, PasswordAction.PASSWORD_BREACH_CHECK, context);
        activityLog.setActionDetails("Password found in breach database: " + found);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password policy violation
     */
    @Transactional
    public ActivityLog logPasswordPolicyViolation(User user, String violation, Device device, RequestContext context) {
        return logFailure(user, device, PasswordAction.PASSWORD_POLICY_VIOLATION, violation, context);
    }

    // ================== PASSWORD SECURITY LOGGING ==================

    /**
     * Logs password lockout trigger
     */
    @Transactional
    public ActivityLog logPasswordLockoutTriggered(User user, String reason, Device device, RequestContext context) {
        return logFailure(user, device, PasswordAction.PASSWORD_LOCKOUT_TRIGGERED, reason, context);
    }

    /**
     * Logs password attempt failure
     */
    @Transactional
    public ActivityLog logPasswordAttemptFailed(User user, String reason, Device device, RequestContext context) {
        return logFailure(user, device, PasswordAction.PASSWORD_ATTEMPT_FAILED, reason, context);
    }

    /**
     * Logs password retry limit exceeded
     */
    @Transactional
    public ActivityLog logPasswordRetryLimitExceeded(User user, int attemptCount, Device device, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, PasswordAction.PASSWORD_RETRY_LIMIT_EXCEEDED,
                "Too many failed attempts", context);
        activityLog.setActionDetails("Failed attempts: " + attemptCount);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password hint request
     */
    @Transactional
    public ActivityLog logPasswordHintRequested(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_HINT_REQUESTED, context);
    }

    /**
     * Logs password recovery initiation
     */
    @Transactional
    public ActivityLog logPasswordRecoveryInitiated(User user, String method, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, PasswordAction.PASSWORD_RECOVERY_INITIATED, context);
        activityLog.setActionDetails("Recovery method: " + method);
        return activityLogRepository.save(activityLog);
    }

    // ================== PASSWORD POLICY LOGGING ==================

    /**
     * Logs password policy update
     */
    @Transactional
    public ActivityLog logPasswordPolicyUpdated(User admin, String policyDetails, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, PasswordAction.PASSWORD_POLICY_UPDATED, context);
        activityLog.setActionDetails("Policy changes: " + policyDetails);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password expiry warning sent
     */
    @Transactional
    public ActivityLog logPasswordExpiryWarningSent(User user, int daysUntilExpiry, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, PasswordAction.PASSWORD_EXPIRY_WARNING_SENT, context);
        activityLog.setActionDetails("Days until expiry: " + daysUntilExpiry);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password rotation requirement
     */
    @Transactional
    public ActivityLog logPasswordRotationRequired(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_ROTATION_REQUIRED, context);
    }

    /**
     * Logs master password change
     */
    @Transactional
    public ActivityLog logMasterPasswordChanged(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.MASTER_PASSWORD_CHANGED, context);
    }

    // ================== ADMIN PASSWORD ACTIONS LOGGING ==================

    /**
     * Logs password reset by administrator
     */
    @Transactional
    public ActivityLog logPasswordAdminReset(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, PasswordAction.PASSWORD_ADMIN_RESET, context);
        activityLog.setActionDetails("Reset password for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password unlock by administrator
     */
    @Transactional
    public ActivityLog logPasswordAdminUnlock(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, PasswordAction.PASSWORD_ADMIN_UNLOCK, context);
        activityLog.setActionDetails("Unlocked password for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password expiry by administrator
     */
    @Transactional
    public ActivityLog logPasswordAdminExpire(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, PasswordAction.PASSWORD_ADMIN_EXPIRE, context);
        activityLog.setActionDetails("Expired password for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }
}