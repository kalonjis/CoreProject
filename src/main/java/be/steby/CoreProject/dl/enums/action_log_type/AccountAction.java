package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * Account lifecycle domain actions.
 *
 * <p>Each constant maps 1-to-1 to an account domain event:
 * <ul>
 *   <li>{@code ACCOUNT_SIGNUP}                 ← SelfSignupCompletedEvent</li>
 *   <li>{@code ACCOUNT_ACTIVATED}              ← AccountConfirmationEvent</li>
 *   <li>{@code ACCOUNT_ACTIVATION_RESENT}      ← RequestAccountActivationEvent</li>
 *   <li>{@code ACCOUNT_DEACTIVATION_REQUESTED} ← RequestAccountDeactivationEvent</li>
 *   <li>{@code ACCOUNT_DEACTIVATED}            ← AccountDeactivationConfirmedEvent</li>
 *   <li>{@code ACCOUNT_REACTIVATION_REQUESTED} ← RequestAccountReactivationEvent</li>
 *   <li>{@code ACCOUNT_REACTIVATED}            ← AccountReactivationConfirmedEvent</li>
 * </ul>
 */
public enum AccountAction implements ActionLogType {

    // =========================================================================
    // Signup
    // =========================================================================

    ACCOUNT_SIGNUP("New account created — awaiting email confirmation"),

    // =========================================================================
    // Activation
    // =========================================================================

    ACCOUNT_ACTIVATED("Account successfully activated"),
    ACCOUNT_ACTIVATION_RESENT("Account activation email resent"),

    // =========================================================================
    // Deactivation
    // =========================================================================

    ACCOUNT_DEACTIVATION_REQUESTED("Account deactivation requested — confirmation email sent"),
    ACCOUNT_DEACTIVATED("Account successfully deactivated"),

    // =========================================================================
    // Reactivation
    // =========================================================================

    ACCOUNT_REACTIVATION_REQUESTED("Account reactivation requested — confirmation email sent"),
    ACCOUNT_REACTIVATED("Account successfully reactivated");

    // =========================================================================

    private final String description;

    AccountAction(String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "ACCOUNT";
    }
}