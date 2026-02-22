package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * Admin domain actions for activity logging.
 *
 * <p>Unlike other domains with a fixed category, AdminAction uses dynamic
 * sub-categories to enable granular filtering while keeping a single enum:</p>
 * <ul>
 *   <li>{@code ADMIN_USER}     — user lifecycle operations</li>
 *   <li>{@code ADMIN_ROLE}     — role grant/revoke</li>
 *   <li>{@code ADMIN_SECURITY} — security-related admin actions</li>
 *   <li>{@code ADMIN_AUDIT}    — audit log access and data export</li>
 *   <li>{@code ADMIN_SYSTEM}   — system configuration</li>
 * </ul>
 *
 * <p>Each constant maps to an admin domain event and stores both the
 * sub-category and the target user (if applicable) in the activity log.</p>
 *
 * @see ActionLogType
 */
public enum AdminAction implements ActionLogType {

    // =========================================================================
    // User Management (ADMIN_USER)
    // =========================================================================

    /** Admin created a new user account. */
    ADMIN_USER_CREATED("ADMIN_USER", "User account created by administrator"),

    /** Admin deleted a user account. */
    ADMIN_USER_DELETED("ADMIN_USER", "User account deleted by administrator"),

    /** Admin manually activated a user account. */
    ADMIN_USER_ACTIVATED("ADMIN_USER", "User account activated by administrator"),

    /** Admin deactivated a user account. */
    ADMIN_USER_DEACTIVATED("ADMIN_USER", "User account deactivated by administrator"),

    /** Admin reactivated a previously deactivated account. */
    ADMIN_USER_REACTIVATED("ADMIN_USER", "User account reactivated by administrator"),

    // =========================================================================
    // Role Management (ADMIN_ROLE)
    // =========================================================================

    /** Admin granted a role to a user. */
    ADMIN_ROLE_GRANTED("ADMIN_ROLE", "Role granted to user by administrator"),

    /** Admin revoked a role from a user. */
    ADMIN_ROLE_REVOKED("ADMIN_ROLE", "Role revoked from user by administrator"),

    // =========================================================================
    // Security Actions (ADMIN_SECURITY)
    // =========================================================================

    /** Admin initiated a password reset for a user. */
    ADMIN_PASSWORD_RESET("ADMIN_SECURITY", "Password reset initiated by administrator"),

    /** Admin forced logout of a user's session(s). */
    ADMIN_FORCE_LOGOUT("ADMIN_SECURITY", "User session(s) terminated by administrator"),

    /** Admin unlocked a locked account. */
    ADMIN_ACCOUNT_UNLOCKED("ADMIN_SECURITY", "User account unlocked by administrator"),

    /** Admin blocked/banned a user. */
    ADMIN_USER_BLOCKED("ADMIN_SECURITY", "User blocked by administrator"),

    /** Admin unblocked a user. */
    ADMIN_USER_UNBLOCKED("ADMIN_SECURITY", "User unblocked by administrator"),

    // =========================================================================
    // Audit & Data (ADMIN_AUDIT)
    // =========================================================================

    /** Admin accessed audit logs. */
    ADMIN_AUDIT_ACCESSED("ADMIN_AUDIT", "Audit logs accessed by administrator"),

    /** Admin exported user data. */
    ADMIN_DATA_EXPORTED("ADMIN_AUDIT", "User data exported by administrator"),

    /** Admin searched user records. */
    ADMIN_USER_SEARCHED("ADMIN_AUDIT", "User search performed by administrator"),

    // =========================================================================
    // System Configuration (ADMIN_SYSTEM)
    // =========================================================================

    /** Admin changed system configuration. */
    ADMIN_CONFIG_CHANGED("ADMIN_SYSTEM", "System configuration changed by administrator");

    // =========================================================================

    private final String subCategory;
    private final String description;

    AdminAction(String subCategory, String description) {
        this.subCategory = subCategory;
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

    /**
     * Returns the sub-category for this admin action.
     * <p>Stored in {@code action_category} column, enabling filtering by
     * admin sub-domain (e.g., "ADMIN_USER", "ADMIN_ROLE").</p>
     */
    @Override
    public String getCategory() {
        return subCategory;
    }

    /**
     * Returns the parent category for grouping all admin actions.
     * <p>Useful for queries like "all admin activity" regardless of sub-category.</p>
     */
    public String getParentCategory() {
        return "ADMIN";
    }

    /**
     * Checks if this action targets another user (vs system-wide action).
     */
    public boolean isUserTargeted() {
        return this != ADMIN_CONFIG_CHANGED && this != ADMIN_AUDIT_ACCESSED;
    }
}