package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.bll.domains.password.models.ForgotPasswordBLLRequest;
import be.steby.CoreProject.dl.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for initiating a password reset process (forgot password flow).
 *
 * <p>This request supports both email and SMS delivery channels. The user's email
 * address is always required as the primary identifier, but they can choose to
 * receive the reset code via SMS if they have a verified phone number.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Response is always generic to prevent email enumeration</li>
 *   <li>No indication whether the email exists in the database</li>
 *   <li>SMS delivery requires verified phone number</li>
 *   <li>Rate limiting should be applied at the controller level</li>
 * </ul>
 *
 * <p>Validations:
 * <ul>
 *   <li>Email must be provided and in valid format</li>
 *   <li>Notification type must be specified</li>
 * </ul>
 *
 * <p>Usage: POST /api/password/forgot
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email address is required")
        @Size(max = 254, message = "Email address cannot exceed 254 characters")
        @Email
        String email,

        @NotNull(message = "Notification type is required")
        NotificationType notificationType
) {
    /**
     * Normalizes the email to lowercase for consistent processing.
     *
     * @return normalized email address
     */
    public String normalizedEmail() {
        return email != null ? email.toLowerCase().trim() : null;
    }

    /**
     * Converts this PL request model to the BLL model.
     *
     * <p>Performs data normalization and passes only business-relevant data
     * to the service layer, maintaining proper separation of concerns.
     *
     * @return BLL model ready for service layer processing
     */
    public ForgotPasswordBLLRequest toBllModel() {
        return new ForgotPasswordBLLRequest(normalizedEmail(), notificationType);
    }
}