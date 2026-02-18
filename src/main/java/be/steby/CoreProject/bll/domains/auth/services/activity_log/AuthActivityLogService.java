package be.steby.CoreProject.bll.domains.auth.services.activity_log;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.auth.events.*;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.AuthAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Auth domain activity log service.
 *
 * <p>Translates auth domain events into {@code ActivityLog} persistence calls
 * via the generic {@link ActivityLogService} base. Each method maps one-to-one
 * to an {@link AuthAction} enum value, keeping the mapping explicit and easy
 * to trace.</p>
 *
 * <p>Methods in this service are called exclusively from
 * {@code AuthActivityListener}, which already runs on the
 * {@code activityLogExecutor} thread pool. The {@code @Async} annotation on
 * the base class methods is therefore a no-op in that context, but provides
 * a safety net if this service is ever called directly from a synchronous
 * path.</p>
 */
@Service
@Slf4j
public class AuthActivityLogService extends ActivityLogService {

    public AuthActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "AUTH";
    }

    // =========================================================================
    // Login / Logout
    // =========================================================================

    /**
     * Persists an {@link AuthAction#LOGIN} entry for a successful login.
     *
     * @param event the successful login event; user and device are guaranteed
     *              non-null by the publisher
     */
    public void logSuccessfulLogin(UserLoggedInEvent event) {
        logUserActivity(event.user(), event.device(), AuthAction.LOGIN, true);
        log.debug("LOGIN logged — user: {}, device: {}",
                event.user().getUsername(), event.device().getId());
    }

    /**
     * Persists an {@link AuthAction#LOGIN_FAILED} entry for a failed login
     * attempt on a known account.
     *
     * @param event the failed login event; user is non-null (existing account),
     *              device may be null in rare edge cases
     */
    public void logFailedLogin(UserLoginFailedEvent event) {
        logUserActivity(event.user(), event.device(), AuthAction.LOGIN_FAILED, false,
                event.failureReason());
        log.debug("LOGIN_FAILED logged — user: {}, reason: {}",
                event.user().getUsername(), event.failureReason());
    }

    /**
     * Persists an {@link AuthAction#LOGOUT} entry for a voluntary logout.
     *
     * @param event the logout event; device may be null if the logout was
     *              triggered across all sessions (e.g. password change)
     */
    public void logUserLogout(UserLogoutEvent event) {
        logUserActivity(event.user(), event.device(), AuthAction.LOGOUT, true);
        log.debug("LOGOUT logged — user: {}, device: {}",
                event.user().getUsername(),
                event.device() != null ? event.device().getId() : "all");
    }

    // =========================================================================
    // Two-Factor Authentication
    // =========================================================================

    /**
     * Persists a {@link AuthAction#TWO_FACTOR_FAILED} entry when the user
     * provides an incorrect or expired 2FA code during login.
     *
     * @param event contains the user, device, 2FA method type and failure reason
     */
    public void logTwoFactorFailed(TwoFactorFailedEvent event) {
        String reason = String.format("[%s] %s",
                event.twoFactorType().name(), event.failureReason());
        logUserActivity(event.user(), event.device(), AuthAction.TWO_FACTOR_FAILED, false, reason);
        log.debug("TWO_FACTOR_FAILED logged — user: {}, method: {}, reason: {}",
                event.user().getUsername(), event.twoFactorType(), event.failureReason());
    }

    // =========================================================================
    // Security — Locking & Blocking
    // =========================================================================

    /**
     * Persists an {@link AuthAction#ACCOUNT_LOCKED} entry at the moment
     * a user account is locked due to exceeding the failed attempt threshold.
     *
     * @param event contains the user, device and the computed unlock time
     *              (null when the lock is indefinite)
     */
    public void logAccountLocked(AccountLockedEvent event) {
        String reason = event.unlockTime() != null
                ? "Account locked until " + event.unlockTime()
                : "Account locked indefinitely";
        logUserActivity(event.user(), event.device(), AuthAction.ACCOUNT_LOCKED, false, reason);
        log.debug("ACCOUNT_LOCKED logged — user: {}, unlockTime: {}",
                event.user().getUsername(), event.unlockTime());
    }

    /**
     * Persists a {@link AuthAction#LOGIN_BLOCKED} entry when a login attempt
     * is rejected upfront by a rate-limiting or blocking rule, before
     * credentials are evaluated.
     *
     * <p>User may be null for pure IP-based blocks where the supplied username
     * does not correspond to any existing account. In that case the log entry
     * is recorded as a security event (no user association).</p>
     *
     * @param event contains the optional user, optional device and block reason
     */
    public void logLoginBlocked(LoginBlockedEvent event) {
        logUserActivity(event.user(), event.device(), AuthAction.LOGIN_BLOCKED, false,
                event.blockReason());
        log.debug("LOGIN_BLOCKED logged — user: {}, reason: {}",
                event.user() != null ? event.user().getUsername() : "unknown",
                event.blockReason());
    }

    // =========================================================================
    // Force Logout
    // =========================================================================

    /**
     * Persists an {@link AuthAction#FORCE_LOGOUT} entry when all active
     * sessions of a user are terminated by a system or admin action.
     *
     * <p>No device reference is included because the logout targets all
     * sessions simultaneously.</p>
     *
     * @param event contains the user and the human-readable reason for the
     *              forced logout
     */
    public void logForceLogout(ForceLogoutEvent event) {
        logUserActivity(event.user(), null, AuthAction.FORCE_LOGOUT, true, event.reason());
        log.debug("FORCE_LOGOUT logged — user: {}, reason: {}",
                event.user().getUsername(), event.reason());
    }
}