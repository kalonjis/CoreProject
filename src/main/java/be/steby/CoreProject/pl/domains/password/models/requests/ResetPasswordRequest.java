package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.bll.domains.password.models.PasswordResetRequest;
import be.steby.CoreProject.pl.domains.password.validators.PasswordMatch;
import be.steby.CoreProject.pl.domains.password.validators.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for completing a password reset with a valid token.
 *
 * <p>This is used when a user clicks on the password reset link from their email
 * and submits a new password. The token is passed as a query parameter.
 *
 * <p>Validations:
 * <ul>
 *   <li>Password must meet strength requirements</li>
 *   <li>Password confirmation must match</li>
 * </ul>
 *
 * <p>Usage: PUT /api/password/reset?token=xxx
 */
@PasswordMatch
public record ResetPasswordRequest(
        @NotBlank(message = "New password cannot be empty")
        @NotNull(message = "New password is required")
        @Size(min = 8, max = 55, message = "New password must be between 8 and 55 characters")
        @StrongPassword
        String password,

        @NotBlank(message = "Password confirmation cannot be empty")
        @NotNull(message = "Password confirmation is required")
        String confirmPassword
) {
    /**
     * Converts this PL request model to a BLL model.
     * Only passes the password field (token is handled separately).
     *
     * @return BLL model ready for service layer processing
     */
    public PasswordResetRequest toBllModel() {
        return new PasswordResetRequest(password);
    }
}