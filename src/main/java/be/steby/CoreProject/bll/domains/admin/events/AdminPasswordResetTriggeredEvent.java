package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.AdminPasswordResetStrategy;

import java.time.Instant;

/**
 * Event emitted when an administrator triggers a password reset for a user.
 * This event contains all information necessary for audit trail and notifications
 * related to admin-initiated password resets.
 *
 * The event includes the reset strategy which determines:
 * - How the reset is processed
 * - What notifications are sent
 * - What security measures are applied
 * - Priority level for handling
 */
public record AdminPasswordResetTriggeredEvent(
        /**
         * The user for whom the password reset is triggered.
         */
        User targetUser,

        /**
         * The administrator who triggered the reset.
         */
        User adminUser,

        /**
         * Reason for the password reset (required for audit).
         */
        String reason,

        /**
         * Reset strategy determining how the operation is executed.
         */
        AdminPasswordResetStrategy strategy,

        /**
         * Whether user will be forced to change password on next login.
         * True for TEMPORARY_PASSWORD and FORCE_EXPIRE strategies.
         */
        boolean forceChangeOnNextLogin,

        /**
         * Whether active sessions should be invalidated.
         * Always true for SECURITY_BREACH strategy.
         */
        boolean invalidateActiveSessions,

        /**
         * Email address where notification was sent.
         * May be different from user's primary email for alternative delivery.
         */
        String notificationEmail,

        /**
         * Event timestamp.
         */
        Instant timestamp
) {

    /**
     * Constructor with automatic timestamp.
     */
    public AdminPasswordResetTriggeredEvent(
            User targetUser,
            User adminUser,
            String reason,
            AdminPasswordResetStrategy strategy,
            boolean forceChangeOnNextLogin,
            boolean invalidateActiveSessions,
            String notificationEmail) {
        this(targetUser, adminUser, reason, strategy, forceChangeOnNextLogin,
                invalidateActiveSessions, notificationEmail, Instant.now());
    }

    /**
     * Creates an admin password reset event.
     *
     * @param targetUser Target user
     * @param adminUser Administrator
     * @param reason Reset reason
     * @param strategy Reset strategy
     * @param forceChangeOnNextLogin Whether to force password change
     * @param invalidateActiveSessions Whether to invalidate sessions
     * @param notificationEmail Notification email address
     * @return New event instance
     */
    public static AdminPasswordResetTriggeredEvent of(
            User targetUser,
            User adminUser,
            String reason,
            AdminPasswordResetStrategy strategy,
            boolean forceChangeOnNextLogin,
            boolean invalidateActiveSessions,
            String notificationEmail) {
        return new AdminPasswordResetTriggeredEvent(
                targetUser, adminUser, reason, strategy, forceChangeOnNextLogin,
                invalidateActiveSessions, notificationEmail
        );
    }

    /**
     * Creates a simple reset event with default settings.
     *
     * @param targetUser Target user
     * @param adminUser Administrator
     * @return New event instance with STANDARD_RESET strategy
     */
    public static AdminPasswordResetTriggeredEvent simple(
            User targetUser,
            User adminUser) {
        return new AdminPasswordResetTriggeredEvent(
                targetUser, adminUser, "Admin-initiated password reset",
                AdminPasswordResetStrategy.STANDARD_RESET,
                false, false, targetUser.getEmail()
        );
    }

    // ===============================
    // BUSINESS LOGIC METHODS
    // ===============================

    /**
     * Checks if the reset is for security reasons.
     * True if strategy is SECURITY_BREACH or reason contains security keywords.
     *
     * @return true if security-related
     */
    public boolean isSecurityRelated() {
        if (strategy == AdminPasswordResetStrategy.SECURITY_BREACH) {
            return true;
        }

        if (reason == null) return false;

        String lowerReason = reason.toLowerCase();
        return lowerReason.contains("security") ||
                lowerReason.contains("breach") ||
                lowerReason.contains("compromised") ||
                lowerReason.contains("suspicious") ||
                lowerReason.contains("hack") ||
                lowerReason.contains("unauthorized") ||
                lowerReason.contains("attack");
    }

    /**
     * Checks if this is an urgent reset requiring immediate attention.
     * True for SECURITY_BREACH with session invalidation.
     *
     * @return true if urgent reset
     */
    public boolean isUrgentReset() {
        return strategy == AdminPasswordResetStrategy.SECURITY_BREACH
                && invalidateActiveSessions;
    }

    /**
     * Checks if this is a security breach reset.
     *
     * @return true if strategy is SECURITY_BREACH
     */
    public boolean isSecurityBreach() {
        return strategy == AdminPasswordResetStrategy.SECURITY_BREACH;
    }

    /**
     * Checks if a temporary password was generated.
     *
     * @return true if strategy is TEMPORARY_PASSWORD
     */
    public boolean isTemporaryPassword() {
        return strategy == AdminPasswordResetStrategy.TEMPORARY_PASSWORD;
    }

    // ===============================
    // GETTER CONVENIENCE METHODS
    // ===============================

    /**
     * Gets target user ID.
     *
     * @return Target user ID
     */
    public Long getTargetUserId() {
        return targetUser.getId();
    }

    /**
     * Gets admin user ID.
     *
     * @return Admin user ID
     */
    public Long getAdminUserId() {
        return adminUser.getId();
    }

    /**
     * Gets target username.
     *
     * @return Target username
     */
    public String getTargetUsername() {
        return targetUser.getUsername();
    }

    /**
     * Gets admin username.
     *
     * @return Admin username
     */
    public String getAdminUsername() {
        return adminUser.getUsername();
    }

    /**
     * Checks if a reason was provided.
     *
     * @return true if reason is present
     */
    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }

    /**
     * Checks if a notification email was specified.
     *
     * @return true if email is present
     */
    public boolean hasNotificationEmail() {
        return notificationEmail != null && !notificationEmail.isBlank();
    }

    /**
     * Checks if reset concerns a user with administrative roles.
     *
     * @return true if user has admin roles
     */
    public boolean isAdministrativeUser() {
        return targetUser.getUserRoles().stream().anyMatch(role ->
                role.name().contains("ADMIN") || role.name().contains("MODERATOR")
        );
    }

    // ===============================
    // DESCRIPTION AND SUMMARY METHODS
    // ===============================

    /**
     * Creates a textual description of the event.
     *
     * @return Event description
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder(
                String.format("Password reset triggered for user '%s' by administrator '%s'",
                        getTargetUsername(), getAdminUsername())
        );

        desc.append(" [Strategy: ").append(strategy.name()).append("]");

        if (hasReason()) {
            desc.append(". Reason: ").append(reason);
        }

        if (forceChangeOnNextLogin) {
            desc.append(" - Password change required on next login");
        }

        if (invalidateActiveSessions) {
            desc.append(" - Active sessions invalidated");
        }

        return desc.toString();
    }

    /**
     * Gets the priority level of the reset.
     *
     * @return Priority level (LOW, MEDIUM, HIGH, URGENT)
     */
    public String getPriorityLevel() {
        return switch (strategy) {
            case SECURITY_BREACH -> invalidateActiveSessions ? "URGENT" : "HIGH";
            case TEMPORARY_PASSWORD -> "MEDIUM";
            case FORCE_EXPIRE -> "LOW-MEDIUM";
            case STANDARD_RESET -> "LOW";
        };
    }

    /**
     * Creates a summary of actions performed.
     *
     * @return Action summary
     */
    public String getActionSummary() {
        StringBuilder summary = new StringBuilder("Password reset");

        summary.append(" [").append(strategy.name()).append("]");

        if (strategy == AdminPasswordResetStrategy.SECURITY_BREACH) {
            summary.append(" + Password revoked");
        }

        if (forceChangeOnNextLogin) {
            summary.append(" + Forced password change");
        }

        if (invalidateActiveSessions) {
            summary.append(" + Sessions invalidated");
        }

        if (hasNotificationEmail()) {
            summary.append(" + Email notification");
        }

        return summary.toString();
    }

    /**
     * Gets security level description.
     *
     * @return Security level
     */
    public String getSecurityLevel() {
        return strategy.getSecurityLevel();
    }
}