package be.steby.CoreProject.dl.enums;

import lombok.Getter;

/**
 * Enumeration of possible reasons for user account deactivation.
 * Each reason has a display name and indicates whether reactivation is allowed.
 */
public enum DeactivationReason {

    // ✅ Self-deactivations that ALLOW reactivation
    TAKING_A_BREAK("Taking a break"),
    TOO_MUCH_TIME("Too much time spent on the application"),
    PRIVACY_CONCERNS("Privacy concerns"),
    ACCOUNT_CLEANUP("Account cleanup"),
    SWITCHING_ACCOUNTS("Switching to another account"),
    WORK_REQUIREMENTS("Work requirements"),
    NOT_USEFUL("Application no longer useful"),
    OTHER("Other reason");

    /**
     * -- GETTER --
     *  Gets the human-readable display name for this deactivation reason.
     *
     * @return The display name
     */
    @Getter
    private final String displayName;


    /**
     * Constructor with display name and reactivation allowance.
     *
     * @param displayName The human-readable name for this deactivation reason
     */
    DeactivationReason(String displayName) {
        this.displayName = displayName;
    }

    /**
     * All deactivation reasons allow reactivation by definition.
     * This method is kept for API compatibility with existing consumers
     *
     * @return always {@code true}
     */
    public boolean allowsReactivation() {
        return true;
    }

}