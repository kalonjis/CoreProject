package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.bll.domains.password.models.ForgotPasswordBLLRequest;
import be.steby.CoreProject.dl.enums.PasswordResetType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for initiating a password reset process (forgot password flow).
 *
 * <p>This request supports multiple password reset delivery methods:
 * <ul>
 *   <li>{@link PasswordResetType#EMAIL_LINK}: Traditional email with clickable reset link</li>
 *   <li>{@link PasswordResetType#EMAIL_CODE}: Email with 6-digit verification code</li>
 *   <li>{@link PasswordResetType#SMS_CODE}: SMS with 6-digit verification code</li>
 * </ul>
 *
 * <p>The user's email address is always required as the primary identifier,
 * regardless of the chosen delivery method.
 *
 * <p><strong>Security considerations:</strong>
 * <ul>
 *   <li>Response is always generic to prevent email enumeration</li>
 *   <li>No indication whether the email exists in the database</li>
 *   <li>SMS delivery requires verified phone number (fails silently if not met)</li>
 *   <li>Rate limiting should be applied at the controller level</li>
 * </ul>
 *
 * <p><strong>Validations:</strong>
 * <ul>
 *   <li>Email must be provided and in valid format</li>
 *   <li>Reset type must be specified</li>
 * </ul>
 *
 * <p>Usage: POST /api/password/forgot
 *
 * @see PasswordResetType
 * @see ForgotPasswordBLLRequest
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email address is required")
        @Size(max = 254, message = "Email address cannot exceed 254 characters")
        @Email(message = "Email address must be valid")
        String email,

        @NotNull(message = "Reset type is required")
        PasswordResetType resetType

) {
    /**
     * Normalizes the email to lowercase for consistent processing.
     *
     * @return normalized email address, or {@code null} if email is null
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
        return new ForgotPasswordBLLRequest(normalizedEmail(), resetType);
    }
}