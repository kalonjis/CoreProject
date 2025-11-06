package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.bll.domains.password.models.ResetPasswordWithPermissionBLLRequest;
import be.steby.CoreProject.pl.domains.password.validators.PasswordMatch;
import be.steby.CoreProject.pl.domains.password.validators.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for completing password reset with SMS permission token.
 *
 * <p>This is used after a user has successfully verified their SMS code
 * and received a permission token. The permission token grants temporary
 * access to reset the password without needing the original email token.
 *
 * <p>Validations:
 * <ul>
 *   <li>Permission token must be provided</li>
 *   <li>Password must meet strength requirements</li>
 *   <li>Password confirmation must match</li>
 * </ul>
 *
 * <p>Usage: PUT /api/password/reset-with-permission
 */
@PasswordMatch(first = "newPassword", second = "confirmNewPassword")
public record ResetPasswordWithPermissionRequest(

        @NotBlank(message = "New password cannot be empty")
        @NotNull(message = "New password is required")
        @Size(min = 8, max = 55, message = "New password must be between 8 and 55 characters")
        @StrongPassword
        String newPassword,

        @NotBlank(message = "Password confirmation cannot be empty")
        @NotNull(message = "Password confirmation is required")
        String confirmNewPassword
) {
    
    /**
     * Converts this PL request model to a BLL model.
     * Only passes the business-relevant fields (excludes confirmPassword).
     *
     * @return BLL model ready for service layer processing
     */
    public ResetPasswordWithPermissionBLLRequest toBllModel(String permissionToken ) {
        return new ResetPasswordWithPermissionBLLRequest(permissionToken, newPassword);
    }
}