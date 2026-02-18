package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.domains.auth.events.*;
import be.steby.CoreProject.bll.domains.auth.services.activity_log.AuthActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for authentication-related activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link AuthActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>Order(100) ensures this listener runs after any security or cache listeners
 * that may act on the same events at higher priority.</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class AuthActivityListener {

    private final AuthActivityLogService authActivityLogService;

    // =========================================================================
    // Login / Logout
    // =========================================================================

    /**
     * Logs a successful user login.
     *
     * @param event published by {@code AuthServiceImpl} after credentials and
     *              device checks pass
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserLoggedIn(UserLoggedInEvent event) {
        try {
            log.debug("Processing login event for user: {}", event.user().getUsername());
            authActivityLogService.logSuccessfulLogin(event);
        } catch (Exception e) {
            log.error("Failed to log successful login for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Logs a failed login attempt for a known user.
     *
     * @param event published by {@code AuthServiceImpl} after credential validation
     *              fails for an existing account
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserLoginFailed(UserLoginFailedEvent event) {
        try {
            log.debug("Processing failed login event for user: {}", event.user().getUsername());
            authActivityLogService.logFailedLogin(event);
        } catch (Exception e) {
            log.error("Failed to log failed login for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Logs a voluntary user logout.
     *
     * @param event published by {@code AuthServiceImpl.logout()}
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserLoggedOut(UserLogoutEvent event) {
        try {
            log.debug("Processing logout event for user: {}", event.user().getUsername());
            authActivityLogService.logUserLogout(event);
        } catch (Exception e) {
            log.error("Failed to log logout for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Two-Factor Authentication
    // =========================================================================

    /**
     * Logs a failed 2FA verification attempt.
     *
     * <p>Repeated failures on the same account may indicate an attacker who
     * has already obtained the primary credentials.</p>
     *
     * @param event published by {@code AuthServiceImpl.verifyTwoFactorAndCompleteLogin()}
     *              when the provided code does not match
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleTwoFactorFailed(TwoFactorFailedEvent event) {
        try {
            log.debug("Processing 2FA failure event for user: {} (method: {})",
                    event.user().getUsername(), event.twoFactorType());
            authActivityLogService.logTwoFactorFailed(event);
        } catch (Exception e) {
            log.error("Failed to log 2FA failure for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Security — Locking & Blocking
    // =========================================================================

    /**
     * Logs the moment an account is locked due to too many failed attempts.
     *
     * <p>This fires once when the lock threshold is crossed, not on every
     * individual failure ({@link UserLoginFailedEvent} covers those).</p>
     *
     * @param event published by {@code AuthServiceImpl.handleAuthenticationFailure()}
     *              immediately after {@code loginAttemptService.recordFailedAttempt()}
     *              tips the account into a blocked state
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAccountLocked(AccountLockedEvent event) {
        try {
            log.debug("Processing account locked event for user: {}", event.user().getUsername());
            authActivityLogService.logAccountLocked(event);
        } catch (Exception e) {
            log.error("Failed to log account lock for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Logs a login attempt that was blocked before credentials were even evaluated.
     *
     * <p>{@code user} may be null when the block is purely IP-based and the
     * supplied username does not resolve to any existing account.</p>
     *
     * @param event published by {@code AuthServiceImpl.handleAuthenticationFailure()}
     *              when an {@code AccountTemporarilyLockedException} is caught
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleLoginBlocked(LoginBlockedEvent event) {
        try {
            String username = event.user() != null ? event.user().getUsername() : "unknown";
            log.debug("Processing login blocked event for user: {}", username);
            authActivityLogService.logLoginBlocked(event);
        } catch (Exception e) {
            log.error("Failed to log blocked login: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Force Logout
    // =========================================================================

    /**
     * Logs a system- or admin-triggered forced logout.
     *
     * <p>Unlike {@link UserLogoutEvent}, this event always targets all active
     * sessions simultaneously and carries no device reference.</p>
     *
     * @param event published by {@code AuthDeactivationListener} on account
     *              deactivation, or by the admin force-logout flow
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleForceLogout(ForceLogoutEvent event) {
        try {
            log.debug("Processing force logout event for user: {}", event.user().getUsername());
            authActivityLogService.logForceLogout(event);
        } catch (Exception e) {
            log.error("Failed to log force logout for user: {}", event.user().getUsername(), e);
        }
    }
}