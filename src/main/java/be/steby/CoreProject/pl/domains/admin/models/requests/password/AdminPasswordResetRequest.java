package be.steby.CoreProject.pl.domains.admin.models.requests.password;

import be.steby.CoreProject.bll.domains.admin.models.password.AdminPasswordResetBLLRequest;
import be.steby.CoreProject.dl.enums.admin.AdminPasswordResetStrategy;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Presentation layer request for admin password reset.
 * Contains Jakarta validation for HTTP request data.
 *
 * This DTO is validated by Spring before reaching the controller,
 * then converted to BLL DTO for business logic processing.
 *
 * Strategy determines the reset behavior:
 * - STANDARD_RESET: Simple email reset
 * - SECURITY_BREACH: Immediate lockout + session invalidation
 * - TEMPORARY_PASSWORD: Generate temp password for alternative delivery
 */
public record AdminPasswordResetRequest(

        @NotNull(message = "Reset strategy is required")
        @Enumerated(EnumType.STRING)
        AdminPasswordResetStrategy strategy,

        @NotBlank(message = "Reason is required for audit purposes")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason,

        boolean invalidateActiveSessions,

        @Email(message = "Alternative notification email must be valid")
        String alternativeNotificationEmail,

        String alternativeDeliveryMethod



) {
    /**
     * Presentation-level validation in constructor.
     * Validates basic rules before passing to BLL.
     */
    public AdminPasswordResetRequest {
        // Presentation validation: SECURITY_BREACH must invalidate sessions
        if (strategy == AdminPasswordResetStrategy.SECURITY_BREACH
                && !invalidateActiveSessions) {
            throw new IllegalArgumentException(
                    "SECURITY_BREACH strategy requires session invalidation");
        }

        // Presentation validation: TEMPORARY_PASSWORD needs alternative channel
        if (strategy == AdminPasswordResetStrategy.TEMPORARY_PASSWORD) {
            if (alternativeNotificationEmail == null
                    && alternativeDeliveryMethod == null) {
                throw new IllegalArgumentException(
                        "TEMPORARY_PASSWORD requires alternative notification channel");
            }
        }
    }

    /**
     * Converts PL request to BLL DTO.
     * Bridges presentation layer to business layer.
     * Performs data cleaning (trim, lowercase) before passing to BLL.
     *
     * @return AdminPasswordResetBLLRequest for BLL processing
     */
    public AdminPasswordResetBLLRequest toBLL() {
        return new AdminPasswordResetBLLRequest(
                strategy,
                reason != null ? reason.trim() : null,
                invalidateActiveSessions,
                alternativeNotificationEmail != null
                        ? alternativeNotificationEmail.trim().toLowerCase()
                        : null,
                alternativeDeliveryMethod != null
                        ? alternativeDeliveryMethod.trim().toUpperCase()
                        : null
        );
    }
}