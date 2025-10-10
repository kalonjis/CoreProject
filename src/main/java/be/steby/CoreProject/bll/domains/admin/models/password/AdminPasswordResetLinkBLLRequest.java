package be.steby.CoreProject.bll.domains.admin.models.password;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminValidationException;

/**
 * Business layer DTO for admin password reset link operation.
 * Contains business-validated data needed for sending password reset links.
 *
 */
public record AdminPasswordResetLinkBLLRequest(
        /**
         * Reason for the password reset link request (required for audit trail).
         * Must be between 10 and 500 characters.
         */
        String reason
) {
    /**
     * Compact constructor with business validation.
     * Validates business rules that go beyond presentation validation.
     */
    public AdminPasswordResetLinkBLLRequest {
        // Validate reason
        if (reason == null || reason.isBlank()) {
            throw new AdminValidationException("Reason is required for admin password reset link");
        }
        if (reason.length() < 10) {
            throw new AdminValidationException(
                    "Reason must be at least 10 characters for audit purposes");
        }
        if (reason.length() > 500) {
            throw new AdminValidationException("Reason cannot exceed 500 characters");
        }
    }
}