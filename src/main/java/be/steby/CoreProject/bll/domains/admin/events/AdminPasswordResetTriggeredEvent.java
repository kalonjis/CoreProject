package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.AdminPasswordResetStrategy;

import java.time.Instant;

/**
 * Event emitted when an administrator triggers a password reset for a user.
 *
 * ✅ SIMPLIFIED for KISS approach:
 * - Removed requestContext (not in constructor parameters)
 * - Simplified constructor signature
 * - Focus on essential audit information
 *
 * This event contains all information necessary for audit trail and notifications
 * related to admin-initiated password resets.
 *
 * @author Steby Core Team
 * @version 2.0 (Simplified KISS approach)
 * @since 2025-01
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
         * Reset strategy determining how the operation is executed.
         */
        AdminPasswordResetStrategy strategy,

        /**
         * Reason for the password reset (required for audit).
         */
        String reason,

        /**
         * Whether active sessions should be invalidated.
         * Always true for SECURITY_BREACH strategy.
         */
        boolean invalidateActiveSessions,

        /**
         * Alternative delivery method for temporary password.
         * Examples: "SMS", "EMAIL", "PHONE"
         */
        String alternativeDeliveryMethod,

        /**
         * Event timestamp.
         */
        Instant timestamp
) {

    // ===============================
    // CONSTRUCTORS
    // ===============================

    /**
     * ✅ PRIMARY CONSTRUCTOR - Used by AdminPasswordServiceImpl
     *
     * Constructor with automatic timestamp.
     */
    public AdminPasswordResetTriggeredEvent(
            User targetUser,
            User adminUser,
            AdminPasswordResetStrategy strategy,
            String reason,
            boolean invalidateActiveSessions,
            String alternativeDeliveryMethod) {
        this(targetUser, adminUser, strategy, reason, invalidateActiveSessions,
                alternativeDeliveryMethod, Instant.now());
    }

    /**
     * Factory method for creating events with all defaults.
     */
    public static AdminPasswordResetTriggeredEvent of(
            User targetUser,
            User adminUser,
            AdminPasswordResetStrategy strategy,
            String reason,
            boolean invalidateActiveSessions,
            String alternativeDeliveryMethod) {
        return new AdminPasswordResetTriggeredEvent(
                targetUser, adminUser, strategy, reason,
                invalidateActiveSessions, alternativeDeliveryMethod
        );
    }

    /**
     * Creates a simple standard reset event.
     */
    public static AdminPasswordResetTriggeredEvent simple(
            User targetUser,
            User adminUser,
            String reason) {
        return new AdminPasswordResetTriggeredEvent(
                targetUser,
                adminUser,
                AdminPasswordResetStrategy.STANDARD_RESET,
                reason,
                false,  // Don't invalidate sessions for standard reset
                null    // No alternative delivery
        );
    }

    // ===============================
    // BUSINESS LOGIC METHODS
    // ===============================

    /**
     * Checks if the reset is for security reasons.
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
     */
    public boolean isUrgentReset() {
        return strategy == AdminPasswordResetStrategy.SECURITY_BREACH
                && invalidateActiveSessions;
    }

    /**
     * Checks if this is a security breach reset.
     */
    public boolean isSecurityBreach() {
        return strategy == AdminPasswordResetStrategy.SECURITY_BREACH;
    }

    /**
     * Checks if a temporary password was generated.
     */
    public boolean isTemporaryPassword() {
        return strategy == AdminPasswordResetStrategy.TEMPORARY_PASSWORD;
    }

    /**
     * Checks if user will be forced to change password.
     * True for TEMPORARY_PASSWORD and FORCE_EXPIRE strategies.
     */
    public boolean forceChangeOnNextLogin() {
        return strategy == AdminPasswordResetStrategy.TEMPORARY_PASSWORD
                || strategy == AdminPasswordResetStrategy.FORCE_EXPIRE;
    }

    // ===============================
    // GETTER CONVENIENCE METHODS
    // ===============================

    public Long getTargetUserId() {
        return targetUser.getId();
    }

    public Long getAdminUserId() {
        return adminUser.getId();
    }

    public String getTargetUsername() {
        return targetUser.getUsername();
    }

    public String getAdminUsername() {
        return adminUser.getUsername();
    }

    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }

    public boolean hasAlternativeDeliveryMethod() {
        return alternativeDeliveryMethod != null && !alternativeDeliveryMethod.isBlank();
    }

    // ===============================
    // DESCRIPTION METHODS
    // ===============================

    /**
     * Creates a textual description of the event for logging.
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

        if (forceChangeOnNextLogin()) {
            desc.append(" - Password change required on next login");
        }

        if (invalidateActiveSessions) {
            desc.append(" - Active sessions invalidated");
        }

        if (hasAlternativeDeliveryMethod()) {
            desc.append(" - Delivery via: ").append(alternativeDeliveryMethod);
        }

        return desc.toString();
    }

    /**
     * Gets the priority level of the reset.
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
     * Gets security level description.
     */
    public String getSecurityLevel() {
        return strategy.getSecurityLevel();
    }

    /**
     * Creates a summary of actions performed.
     */
    public String getActionSummary() {
        StringBuilder summary = new StringBuilder("Password reset");

        summary.append(" [").append(strategy.name()).append("]");

        if (strategy == AdminPasswordResetStrategy.SECURITY_BREACH) {
            summary.append(" + Account locked");
        }

        if (forceChangeOnNextLogin()) {
            summary.append(" + Forced password change");
        }

        if (invalidateActiveSessions) {
            summary.append(" + Sessions invalidated");
        }

        if (hasAlternativeDeliveryMethod()) {
            summary.append(" + ").append(alternativeDeliveryMethod).append(" notification");
        }

        return summary.toString();
    }
}