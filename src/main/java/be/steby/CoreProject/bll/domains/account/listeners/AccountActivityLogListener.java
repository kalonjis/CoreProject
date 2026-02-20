package be.steby.CoreProject.bll.domains.account.listeners;

import be.steby.CoreProject.bll.domains.account.events.*;
import be.steby.CoreProject.bll.domains.account.services.AccountActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for account lifecycle activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link AccountActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after
 * {@link AccountNotificationListener} ({@code @Order(10)}) — a logging failure
 * never blocks email delivery.</p>
 *
 * Covered events → AccountAction mapping:
 * <ul>
 *   <li>{@link SelfSignupCompletedEvent}         → ACCOUNT_SIGNUP</li>
 *   <li>{@link AccountConfirmationEvent}          → ACCOUNT_ACTIVATED</li>
 *   <li>{@link RequestAccountActivationEvent}     → ACCOUNT_ACTIVATION_RESENT</li>
 *   <li>{@link RequestAccountDeactivationEvent}   → ACCOUNT_DEACTIVATION_REQUESTED</li>
 *   <li>{@link AccountDeactivationConfirmedEvent} → ACCOUNT_DEACTIVATED</li>
 *   <li>{@link RequestAccountReactivationEvent}   → ACCOUNT_REACTIVATION_REQUESTED</li>
 *   <li>{@link AccountReactivationConfirmedEvent} → ACCOUNT_REACTIVATED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class AccountActivityLogListener {

    private final AccountActivityLogService accountActivityLogService;

    // =========================================================================
    // Signup
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleSelfSignupCompleted(SelfSignupCompletedEvent event) {
        try {
            log.debug("Processing signup event for user: {}", event.user().getUsername());
            accountActivityLogService.logSignup(event);
        } catch (Exception e) {
            log.error("Failed to log signup for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Activation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleAccountConfirmation(AccountConfirmationEvent event) {
        try {
            log.debug("Processing account activation event for user: {}", event.user().getUsername());
            accountActivityLogService.logActivated(event);
        } catch (Exception e) {
            log.error("Failed to log account activation for user: {}", event.user().getUsername(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestAccountActivation(RequestAccountActivationEvent event) {
        try {
            log.debug("Processing activation resend event for user: {}", event.user().getUsername());
            accountActivityLogService.logActivationResent(event);
        } catch (Exception e) {
            log.error("Failed to log activation resend for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Deactivation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestAccountDeactivation(RequestAccountDeactivationEvent event) {
        try {
            log.debug("Processing deactivation request event for user: {}", event.user().getUsername());
            accountActivityLogService.logDeactivationRequested(event);
        } catch (Exception e) {
            log.error("Failed to log deactivation request for user: {}", event.user().getUsername(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleAccountDeactivationConfirmed(AccountDeactivationConfirmedEvent event) {
        try {
            log.debug("Processing account deactivation confirmed event for user: {}", event.user().getUsername());
            accountActivityLogService.logDeactivated(event);
        } catch (Exception e) {
            log.error("Failed to log account deactivation for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Reactivation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestAccountReactivation(RequestAccountReactivationEvent event) {
        try {
            log.debug("Processing reactivation request event for user: {}", event.user().getUsername());
            accountActivityLogService.logReactivationRequested(event);
        } catch (Exception e) {
            log.error("Failed to log reactivation request for user: {}", event.user().getUsername(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleAccountReactivationConfirmed(AccountReactivationConfirmedEvent event) {
        try {
            log.debug("Processing account reactivation confirmed event for user: {}", event.user().getUsername());
            accountActivityLogService.logReactivated(event);
        } catch (Exception e) {
            log.error("Failed to log account reactivation for user: {}", event.user().getUsername(), e);
        }
    }
}