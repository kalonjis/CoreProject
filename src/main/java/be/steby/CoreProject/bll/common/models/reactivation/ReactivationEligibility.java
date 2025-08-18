// be/steby/CoreProject/bll/common/models/reactivation/ReactivationEligibility.java
package be.steby.CoreProject.bll.common.models.reactivation;

/**
 * ✅ MODEL TRANSVERSAL - Représente l'éligibilité d'un utilisateur à la réactivation
 */
public class ReactivationEligibility {
    private final boolean eligible;
    private final String reason;
    private final EligibilityType type;

    public enum EligibilityType {
        ELIGIBLE,
        NOT_ELIGIBLE,
        NEVER_REACTIVATABLE,
        INSUFFICIENT_PERMISSIONS
    }

    private ReactivationEligibility(boolean eligible, String reason, EligibilityType type) {
        this.eligible = eligible;
        this.reason = reason;
        this.type = type;
    }

    // ===== FACTORY METHODS =====

    public static ReactivationEligibility eligible() {
        return new ReactivationEligibility(true, "Eligible for reactivation", EligibilityType.ELIGIBLE);
    }

    public static ReactivationEligibility notEligible(String reason) {
        return new ReactivationEligibility(false, reason, EligibilityType.NOT_ELIGIBLE);
    }

    public static ReactivationEligibility neverReactivatable(String reason) {
        return new ReactivationEligibility(false, reason, EligibilityType.NEVER_REACTIVATABLE);
    }

    public static ReactivationEligibility insufficientPermissions(String reason) {
        return new ReactivationEligibility(false, reason, EligibilityType.INSUFFICIENT_PERMISSIONS);
    }

    // ===== GETTERS =====

    public boolean isEligible() {
        return eligible;
    }

    public String getReason() {
        return reason;
    }

    public EligibilityType getType() {
        return type;
    }

    public boolean isNeverReactivatable() {
        return type == EligibilityType.NEVER_REACTIVATABLE;
    }

    public boolean isInsufficientPermissions() {
        return type == EligibilityType.INSUFFICIENT_PERMISSIONS;
    }

    @Override
    public String toString() {
        return String.format("ReactivationEligibility{eligible=%s, reason='%s', type=%s}",
                eligible, reason, type);
    }
}