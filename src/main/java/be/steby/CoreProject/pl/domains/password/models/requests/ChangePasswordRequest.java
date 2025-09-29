package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.bll.domains.password.models.PasswordChangeRequest;
import be.steby.CoreProject.pl.domains.password.validators.PasswordMatch;
import be.steby.CoreProject.pl.domains.password.validators.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for changing password for an authenticated user.
 *
 * <p>Validations:
 * <ul>
 *   <li>Current password must be provided</li>
 *   <li>New password must meet strength requirements</li>
 *   <li>Password confirmation must match</li>
 *   <li>New password must be different from current password</li>
 * </ul>
 *
 * <p>Usage: POST /api/password/change
 */
@PasswordMatch
public record ChangePasswordRequest(
        @NotBlank(message = "Current password cannot be empty")
        @NotNull(message = "Current password is required")
        String currentPassword,

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
     * Validates that the new password is different from the current password.
     * Prevents users from "changing" to their existing password.
     */
    @AssertTrue(message = "New password must be different from current password")
    public boolean isPasswordDifferent() {
        return password == null || currentPassword == null ||
                !password.equals(currentPassword);
    }

    /**
     * Converts this PL request model to a BLL model.
     * Only passes necessary business logic fields (excludes confirmPassword).
     *
     * @return BLL model ready for service layer processing
     */
    public PasswordChangeRequest toBllModel() {
        return new PasswordChangeRequest(currentPassword, password);
    }
}