package be.steby.CoreProject.dl.enums;

import lombok.Getter;

/**
 * Enumeration of possible reasons for user account self-deactivation.
 *
 * <p>All values represent reversible deactivations — the account can always
 * be reactivated by the user afterward.
 *
 * <p>GDPR deletion is a separate, irreversible flow tracked via
 * {@link be.steby.CoreProject.dl.entities.User#getGdprDeletedAt()}.
 */
public enum DeactivationReason {

    TAKING_A_BREAK("Taking a break"),
    TOO_MUCH_TIME("Too much time spent on the application"),
    PRIVACY_CONCERNS("Privacy concerns"),
    ACCOUNT_CLEANUP("Account cleanup"),
    SWITCHING_ACCOUNTS("Switching to another account"),
    WORK_REQUIREMENTS("Work requirements"),
    NOT_USEFUL("Application no longer useful"),
    OTHER("Other reason");

    @Getter
    private final String displayName;

    DeactivationReason(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns {@code true} — all self-deactivation reasons allow reactivation.
     *
     * @return always {@code true}
     */
    public boolean allowsReactivation() {
        return true;
    }
}