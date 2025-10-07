package be.steby.CoreProject.bll.domains.admin.models.password;

import be.steby.CoreProject.dl.enums.admin.AdminPasswordResetStrategy;

/**
 * Business layer DTO for admin password reset operation.
 * Contains business-validated data needed for password reset logic.
 *
 * This is separate from PL DTO to maintain layer separation and
 * allow business-level validation without presentation concerns.
 *
 * The strategy determines how the reset is processed:
 * - STANDARD_RESET: Simple email reset link
 * - SECURITY_BREACH: Immediate revocation + session invalidation
 * - TEMPORARY_PASSWORD: Generate temp password for alternative delivery
 * - FORCE_EXPIRE: Mark password as expired for next login
 */
public record AdminPasswordResetBLLRequest(
        /**
         * Reset strategy determining how the operation is executed.
         */
        AdminPasswordResetStrategy strategy,

        /**
         * Reason for the password reset (required for audit trail).
         * Must be between 10 and 500 characters.
         */
        String reason,

        /**
         * Whether to invalidate all active sessions for this user.
         * Always true for SECURITY_BREACH strategy.
         */
        boolean invalidateActiveSessions,

        /**
         * Alternative email to send notification to.
         * If null, uses user's primary email.
         */
        String alternativeNotificationEmail,

        /**
         * Alternative delivery method for temporary password.
         * Used only with TEMPORARY_PASSWORD strategy.
         * Examples: "SMS", "PHONE_CALL", "EMAIL", "IN_PERSON"
         */
        String alternativeDeliveryMethod
) {
    /**
     * Compact constructor with business validation.
     * Validates business rules that go beyond presentation validation.
     */
    public AdminPasswordResetBLLRequest {
        // Validate strategy
        if (strategy == null) {
            throw new IllegalArgumentException("Reset strategy is required");
        }

        // Validate reason
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required for admin password reset");
        }
        if (reason.length() < 10) {
            throw new IllegalArgumentException(
                    "Reason must be at least 10 characters for audit purposes");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Reason cannot exceed 500 characters");
        }

        // Business rule: SECURITY_BREACH must invalidate sessions
        if (strategy == AdminPasswordResetStrategy.SECURITY_BREACH && !invalidateActiveSessions) {
            throw new IllegalArgumentException(
                    "SECURITY_BREACH strategy requires session invalidation");
        }

        // Business rule: TEMPORARY_PASSWORD requires alternative delivery method
        if (strategy == AdminPasswordResetStrategy.TEMPORARY_PASSWORD) {
            if (alternativeNotificationEmail == null && alternativeDeliveryMethod == null) {
                throw new IllegalArgumentException(
                        "TEMPORARY_PASSWORD strategy requires an alternative notification channel");
            }
        }
    }

    /**
     * Checks if reset is for security reasons based on keywords in reason.
     *
     * @return true if reason contains security-related keywords
     */
    public boolean isSecurityRelated() {
        if (strategy == AdminPasswordResetStrategy.SECURITY_BREACH) {
            return true;
        }

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
     * Checks if this is a security breach reset.
     *
     * @return true if strategy is SECURITY_BREACH
     */
    public boolean isSecurityBreach() {
        return strategy == AdminPasswordResetStrategy.SECURITY_BREACH;
    }

    /**
     * Checks if this generates a temporary password.
     *
     * @return true if strategy is TEMPORARY_PASSWORD
     */
    public boolean isTemporaryPassword() {
        return strategy == AdminPasswordResetStrategy.TEMPORARY_PASSWORD;
    }

    /**
     * Checks if an alternative email is provided.
     *
     * @return true if alternative email is present
     */
    public boolean hasAlternativeEmail() {
        return alternativeNotificationEmail != null &&
                !alternativeNotificationEmail.isBlank();
    }

    /**
     * Checks if an alternative delivery method is specified.
     *
     * @return true if alternative delivery method is present
     */
    public boolean hasAlternativeDeliveryMethod() {
        return alternativeDeliveryMethod != null &&
                !alternativeDeliveryMethod.isBlank();
    }

    /**
     * Gets the notification email to use (alternative or default).
     *
     * @param defaultEmail User's primary email
     * @return Email to use for notification
     */
    public String getNotificationEmail(String defaultEmail) {
        return hasAlternativeEmail() ? alternativeNotificationEmail : defaultEmail;
    }
}