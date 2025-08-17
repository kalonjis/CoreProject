package be.steby.CoreProject.dl.enums;

/**
 * Enumeration of possible reasons for user account deactivation.
 * Each reason has a display name and indicates whether reactivation is allowed.
 */
public enum DeactivationReason {

    // ✅ Self-deactivations that ALLOW reactivation
    TAKING_A_BREAK("Taking a break", true),
    TOO_MUCH_TIME("Too much time spent on the application", true),
    PRIVACY_CONCERNS("Privacy concerns", true),
    ACCOUNT_CLEANUP("Account cleanup", true),
    SWITCHING_ACCOUNTS("Switching to another account", true),
    WORK_REQUIREMENTS("Work requirements", true),
    NOT_USEFUL("Application no longer useful", true),
    OTHER("Other reason", true),

    // ❌ Self-deactivations that DO NOT allow reactivation
    GDPR_REQUEST("GDPR deletion request", false);

    private final String displayName;
    private final boolean allowsReactivation;  // ✅ NEW FIELD

    /**
     * Constructor with display name and reactivation allowance.
     *
     * @param displayName The human-readable name for this deactivation reason
     * @param allowsReactivation Whether this reason allows account reactivation
     */
    DeactivationReason(String displayName, boolean allowsReactivation) {
        this.displayName = displayName;
        this.allowsReactivation = allowsReactivation;
    }

    /**
     * Gets the human-readable display name for this deactivation reason.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * ✅ NEW METHOD - Determines if this deactivation reason allows reactivation.
     * Replaces the complex logic in DeactivationMessageService.
     *
     * @return true if reactivation is allowed, false otherwise
     */
    public boolean allowsReactivation() {
        return allowsReactivation;
    }

    /**
     * ✅ UTILITY METHOD - Returns all deactivation reasons that allow reactivation.
     *
     * @return Array of reasons that allow reactivation
     */
    public static DeactivationReason[] getReactivableReasons() {
        return java.util.Arrays.stream(values())
                .filter(DeactivationReason::allowsReactivation)
                .toArray(DeactivationReason[]::new);
    }

    /**
     * ✅ UTILITY METHOD - Returns all deactivation reasons that do NOT allow reactivation.
     *
     * @return Array of reasons that do not allow reactivation
     */
    public static DeactivationReason[] getNonReactivableReasons() {
        return java.util.Arrays.stream(values())
                .filter(reason -> !reason.allowsReactivation())
                .toArray(DeactivationReason[]::new);
    }

    /**
     * Returns the total count of reasons that allow reactivation.
     *
     * @return Count of reactivable reasons
     */
    public static long getReactivableCount() {
        return java.util.Arrays.stream(values())
                .filter(DeactivationReason::allowsReactivation)
                .count();
    }
}