package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.AuthAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Authentication domain activity logging service.
 * PURE logging service - records what happened, nothing more.
 *
 * Principle: "We log events, analysts analyze them"
 *
 * @author Steby Core Project Team
 * @version 2.0 - Pure KISS Edition
 */
@Service
@Slf4j
public class AuthActivityLogService extends AbstractActivityLogService {

    // ================== CONSTRUCTOR ==================

    public AuthActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "AUTH";
    }

    // ================== PURE LOGGING METHODS ==================

    /**
     * Logs a successful login
     */
    @Transactional
    public ActivityLog logLogin(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.LOGIN, context);
    }

    /**
     * Logs a failed login attempt
     */
    @Transactional
    public ActivityLog logLoginFailed(User user, Device device, String reason, RequestContext context) {
        return logFailure(user, device, AuthAction.LOGIN_FAILED, reason, context);
    }

    /**
     * Logs a logout
     */
    @Transactional
    public ActivityLog logLogout(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.LOGOUT, context);
    }

    /**
     * Logs a token refresh
     */
    @Transactional
    public ActivityLog logTokenRefresh(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.TOKEN_REFRESH, context);
    }

    /**
     * Logs two-factor authentication setup
     */
    @Transactional
    public ActivityLog logTwoFactorSetup(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.TWO_FA_SETUP, context);
    }

    /**
     * Logs two-factor authentication verification
     */
    @Transactional
    public ActivityLog logTwoFactorVerification(User user, Device device, boolean successful,
                                                String reason, RequestContext context) {
        if (successful) {
            return logSuccess(user, device, AuthAction.TWO_FA_VERIFICATION, context);
        } else {
            return logFailure(user, device, AuthAction.TWO_FA_VERIFICATION, reason, context);
        }
    }

    /**
     * Logs session expiration
     */
    @Transactional
    public ActivityLog logSessionExpired(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.SESSION_EXPIRED, context);
    }

    /**
     * Logs when account is locked due to failed attempts
     */
    @Transactional
    public ActivityLog logAccountLocked(User user, Device device, String reason, RequestContext context) {
        return logFailure(user, device, AuthAction.ACCOUNT_LOCKED_ATTEMPTS, reason, context);
    }

    /**
     * Logs session termination
     */
    @Transactional
    public ActivityLog logSessionTerminated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.SESSION_TERMINATED, context);
    }

    /**
     * Logs security challenge (replaces SecurityActivityLogService)
     */
    @Transactional
    public ActivityLog logSecurityChallenge(User user, Device device, boolean passed,
                                            String challengeType, RequestContext context) {
        if (passed) {
            ActivityLog activityLog = logSuccess(user, device, AuthAction.SECURITY_CHALLENGE_PASSED, context);
            activityLog.setActionDetails("Challenge type: " + challengeType);
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(user, device, AuthAction.SECURITY_CHALLENGE_FAILED,
                    "Failed challenge: " + challengeType, context);
        }
    }

    /**
     * Logs password-based authentication
     */
    @Transactional
    public ActivityLog logPasswordLogin(User user, Device device, boolean successful,
                                        String failureReason, RequestContext context) {
        if (successful) {
            ActivityLog activityLog = logSuccess(user, device, AuthAction.LOGIN, context);
            activityLog.setActionDetails("Login method: password");
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(user, device, AuthAction.LOGIN_FAILED, failureReason, context);
        }
    }

    /**
     * Logs SSO authentication
     */
    @Transactional
    public ActivityLog logSsoLogin(User user, Device device, boolean successful,
                                   String provider, RequestContext context) {
        if (successful) {
            ActivityLog activityLog = logSuccess(user, device, AuthAction.SSO_LOGIN, context);
            activityLog.setActionDetails("SSO Provider: " + provider);
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(user, device, AuthAction.LOGIN_FAILED, "SSO failed: " + provider, context);
        }
    }
}