package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * Password domain actions for activity logging.
 *
 * <p>Covers both voluntary password changes (user knows current password) and
 * password reset flows (token-based recovery). Each constant maps to a specific
 * domain event for audit and security monitoring.</p>
 *
 * <h3>Password Change (authenticated user)</h3>
 * <ul>
 *   <li>{@code PASSWORD_CHANGED}        — successful password change</li>
 *   <li>{@code PASSWORD_CHANGE_FAILED}  — failed attempt (wrong current password)</li>
 * </ul>
 *
 * <h3>Password Reset (token-based recovery)</h3>
 * <ul>
 *   <li>{@code PASSWORD_RESET_REQUESTED}       — reset email/SMS sent</li>
 *   <li>{@code PASSWORD_RESET_TOKEN_REFRESHED} — new token issued (previous expired)</li>
 *   <li>{@code PASSWORD_RESET_COMPLETED}       — password successfully reset via token</li>
 *   <li>{@code PASSWORD_RESET_FAILED}          — invalid/expired token used</li>
 * </ul>
 *
 * <h3>Admin-initiated</h3>
 * <ul>
 *   <li>{@code PASSWORD_RESET_BY_ADMIN} — admin forced password reset</li>
 * </ul>
 *
 * @see ActionLogType
 */
public enum PasswordAction implements ActionLogType {

    // =========================================================================
    // Password Change (authenticated user)
    // =========================================================================

    /**
     * User successfully changed their password while authenticated.
     * Requires knowledge of the current password.
     */
    PASSWORD_CHANGED("Password successfully changed"),

    /**
     * User attempted to change password but provided incorrect current password.
     * Important for brute-force detection on authenticated sessions.
     */
    PASSWORD_CHANGE_FAILED("Password change failed — incorrect current password"),

    // =========================================================================
    // Password Reset (token-based recovery)
    // =========================================================================

    /**
     * User requested a password reset — email or SMS with token has been sent.
     * May indicate account takeover attempt if not initiated by owner.
     */
    PASSWORD_RESET_REQUESTED("Password reset requested — token sent"),

    /**
     * User requested a new reset token because the previous one expired.
     * Multiple refreshes in short time may indicate delivery issues or attack.
     */
    PASSWORD_RESET_TOKEN_REFRESHED("Password reset token refreshed — new token sent"),

    /**
     * User successfully reset their password using a valid token.
     * Marks completion of the recovery flow.
     */
    PASSWORD_RESET_COMPLETED("Password successfully reset via recovery token"),

    /**
     * Password reset attempt failed — token was invalid, expired, or already used.
     * High volume may indicate brute-force token guessing.
     */
    PASSWORD_RESET_FAILED("Password reset failed — invalid or expired token"),

    // =========================================================================
    // Admin-initiated
    // =========================================================================

    /**
     * Administrator forced a password reset for this user.
     * Logged for audit trail and security compliance.
     */
    PASSWORD_RESET_BY_ADMIN("Password reset initiated by administrator");

    // =========================================================================

    private final String description;

    PasswordAction(String description) {
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
        return "PASSWORD";
    }
}