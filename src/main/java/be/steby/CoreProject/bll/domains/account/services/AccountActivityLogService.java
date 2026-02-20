package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.account.events.*;
import be.steby.CoreProject.bll.domains.account.listeners.AccountActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.AccountAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Account domain activity log service.
 *
 * <p>Translates account domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to an {@link AccountAction} constant.</p>
 *
 * <p>Called exclusively from {@link AccountActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 */
@Service
@Slf4j
public class AccountActivityLogService extends ActivityLogService {

    public AccountActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "ACCOUNT";
    }

    // =========================================================================
    // Signup
    // =========================================================================

    /**
     * Persists an {@link AccountAction#ACCOUNT_SIGNUP} entry when a user
     * completes self-registration. The account is not yet active at this point.
     *
     * @param event contains the newly created user; device is null (no session yet)
     */
    public void logSignup(SelfSignupCompletedEvent event) {
        logUserActivity(event.user(), event.device(), AccountAction.ACCOUNT_SIGNUP, true);
        log.debug("ACCOUNT_SIGNUP logged — user: {}", event.user().getUsername());
    }

    // =========================================================================
    // Activation
    // =========================================================================

    /**
     * Persists an {@link AccountAction#ACCOUNT_ACTIVATED} entry when the user
     * confirms their email and activates their account for the first time.
     *
     * @param event contains the activated user; device is null (token-based flow)
     */
    public void logActivated(AccountConfirmationEvent event) {
        logUserActivity(event.user(), null, AccountAction.ACCOUNT_ACTIVATED, true);
        log.debug("ACCOUNT_ACTIVATED logged — user: {}", event.user().getUsername());
    }

    /**
     * Persists an {@link AccountAction#ACCOUNT_ACTIVATION_RESENT} entry when
     * the user requests a new activation email (e.g. original token expired).
     *
     * @param event contains the user whose activation email was resent
     */
    public void logActivationResent(RequestAccountActivationEvent event) {
        logUserActivity(event.user(), null, AccountAction.ACCOUNT_ACTIVATION_RESENT, true);
        log.debug("ACCOUNT_ACTIVATION_RESENT logged — user: {}", event.user().getUsername());
    }

    // =========================================================================
    // Deactivation
    // =========================================================================

    /**
     * Persists an {@link AccountAction#ACCOUNT_DEACTIVATION_REQUESTED} entry
     * when the authenticated user initiates a deactivation request.
     * The account is still active; a confirmation email has been sent.
     *
     * @param event contains the user, deactivation reason and optional details
     */
    public void logDeactivationRequested(RequestAccountDeactivationEvent event) {
        String detail = buildDeactivationDetail(
                event.deactivationReason() != null ? event.deactivationReason().getDisplayName() : null,
                event.reasonDetails()
        );
        logUserActivity(event.user(), null, AccountAction.ACCOUNT_DEACTIVATION_REQUESTED, true, detail);
        log.debug("ACCOUNT_DEACTIVATION_REQUESTED logged — user: {}, reason: {}",
                event.user().getUsername(), event.deactivationReason());
    }

    /**
     * Persists an {@link AccountAction#ACCOUNT_DEACTIVATED} entry when the
     * user confirms deactivation via the email link.
     *
     * @param event contains the user, confirmed deactivation reason and details
     */
    public void logDeactivated(AccountDeactivationConfirmedEvent event) {
        String detail = buildDeactivationDetail(
                event.deactivationReason() != null ? event.deactivationReason().getDisplayName() : null,
                event.reasonDetails()
        );
        logUserActivity(event.user(), null, AccountAction.ACCOUNT_DEACTIVATED, true, detail);
        log.debug("ACCOUNT_DEACTIVATED logged — user: {}, reason: {}",
                event.user().getUsername(), event.deactivationReason());
    }

    // =========================================================================
    // Reactivation
    // =========================================================================

    /**
     * Persists an {@link AccountAction#ACCOUNT_REACTIVATION_REQUESTED} entry
     * when a disabled user requests account reactivation.
     * A confirmation email has been sent at this point.
     *
     * @param event contains the user requesting reactivation
     */
    public void logReactivationRequested(RequestAccountReactivationEvent event) {
        logUserActivity(event.user(), null, AccountAction.ACCOUNT_REACTIVATION_REQUESTED, true);
        log.debug("ACCOUNT_REACTIVATION_REQUESTED logged — user: {}", event.user().getUsername());
    }

    /**
     * Persists an {@link AccountAction#ACCOUNT_REACTIVATED} entry when the
     * user confirms reactivation via the email link and the account is re-enabled.
     *
     * @param event contains the reactivated user
     */
    public void logReactivated(AccountReactivationConfirmedEvent event) {
        logUserActivity(event.user(), null, AccountAction.ACCOUNT_REACTIVATED, true);
        log.debug("ACCOUNT_REACTIVATED logged — user: {}", event.user().getUsername());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Builds a compact detail string for deactivation-related log entries.
     * Returns null if both arguments are blank to avoid storing empty strings.
     */
    private String buildDeactivationDetail(String reason, String details) {
        if (reason == null && (details == null || details.isBlank())) {
            return null;
        }
        if (details == null || details.isBlank()) {
            return reason;
        }
        return reason != null ? reason + " — " + details : details;
    }
}