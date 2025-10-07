package be.steby.CoreProject.dl.enums.admin;

/**
 * Strategy for admin password reset operations.
 * Determines how the password reset is handled and what security measures are applied.
 *
 * Each strategy has different implications for security, user experience, and audit trail.
 */
public enum AdminPasswordResetStrategy {

    /**
     * Standard reset: Sends reset email, user clicks link, sets new password.
     * Current password remains valid until user completes the reset process.
     *
     * Use case: User forgot password and requested admin assistance.
     *
     * Security level: Low
     * User impact: Minimal - user has full control
     */
    STANDARD_RESET,

    /**
     * Security breach: IMMEDIATELY revokes current password and invalidates all sessions.
     * User CANNOT login until they reset via email link.
     *
     * Use case: Account compromised, suspicious activity detected, security incident.
     *
     * Security level: High
     * User impact: High - user is locked out until reset
     */
    SECURITY_BREACH,

    /**
     * Temporary password: Generates a temporary password sent via alternative channel.
     * Forces user to change password on first login with the temporary password.
     *
     * Use case: Primary email inaccessible, urgent access recovery needed.
     *
     * Security level: Medium
     * User impact: Medium - must use temporary password then change
     */
    TEMPORARY_PASSWORD,

    /**
     * Force expire: Marks password as expired, user must reset on next login.
     * Current password still works for ONE more login only.
     *
     * Use case: Compliance policy (e.g., password older than 90 days).
     *
     * Security level: Low-Medium
     * User impact: Low - user can login once more before forced change
     */
    FORCE_EXPIRE;

    /**
     * Checks if this strategy requires immediate password revocation.
     *
     * @return true if current password should be revoked immediately
     */
    public boolean requiresImmediateRevocation() {
        return this == SECURITY_BREACH;
    }

    /**
     * Checks if this strategy requires session invalidation.
     *
     * @return true if all user sessions should be terminated
     */
    public boolean requiresSessionInvalidation() {
        return this == SECURITY_BREACH;
    }

    /**
     * Checks if this strategy generates a temporary password.
     *
     * @return true if a temporary password should be generated
     */
    public boolean generatesTemporaryPassword() {
        return this == TEMPORARY_PASSWORD;
    }

    /**
     * Gets the security level description for audit purposes.
     *
     * @return Security level as string
     */
    public String getSecurityLevel() {
        return switch (this) {
            case SECURITY_BREACH -> "HIGH";
            case TEMPORARY_PASSWORD -> "MEDIUM";
            case FORCE_EXPIRE -> "LOW-MEDIUM";
            case STANDARD_RESET -> "LOW";
        };
    }

    /**
     * Gets a human-readable description of this strategy.
     *
     * @return Description string
     */
    public String getDescription() {
        return switch (this) {
            case STANDARD_RESET ->
                    "Standard password reset via email link";
            case SECURITY_BREACH ->
                    "Immediate password revocation due to security incident";
            case TEMPORARY_PASSWORD ->
                    "Temporary password generation for alternative delivery";
            case FORCE_EXPIRE ->
                    "Password expiration for compliance or policy enforcement";
        };
    }
}