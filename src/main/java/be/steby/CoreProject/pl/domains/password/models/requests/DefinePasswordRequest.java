package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.pl.domains.password.validators.PasswordMatch;
import be.steby.CoreProject.pl.domains.password.validators.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for defining a password for OAuth-only users.
 *
 * <p>This is used by users who signed up via OAuth (Google, GitHub, Microsoft)
 * and want to also be able to login with credentials.
 *
 * <p>Unlike {@link ChangePasswordRequest}, this does NOT require a current password
 * since OAuth users don't have one yet.
 *
 * <p>Validations:
 * <ul>
 *   <li>Password must meet strength requirements</li>
 *   <li>Password confirmation must match</li>
 * </ul>
 *
 * <p>Usage: PUT /api/password/define
 */
@PasswordMatch
public record DefinePasswordRequest(
        @NotBlank(message = "Password cannot be empty")
        @NotNull(message = "Password is required")
        @Size(min = 8, max = 55, message = "Password must be between 8 and 55 characters")
        @StrongPassword
        String password,

        @NotBlank(message = "Password confirmation cannot be empty")
        @NotNull(message = "Password confirmation is required")
        String confirmPassword
) {}