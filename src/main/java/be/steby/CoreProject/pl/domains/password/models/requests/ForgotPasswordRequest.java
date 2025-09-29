package be.steby.CoreProject.pl.domains.password.models.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for initiating a password reset process (forgot password flow).
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Response is always generic to prevent email enumeration</li>
 *   <li>No indication whether the email exists in the database</li>
 *   <li>Rate limiting should be applied at the controller level</li>
 * </ul>
 *
 * <p>Usage: POST /api/password/forgot
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email address is required")
        @Email(message = "Email address must be valid")
        @Size(max = 100, message = "Email address cannot exceed 100 characters")
        String email
) {
    /**
     * Normalizes the email to lowercase for consistent processing.
     *
     * @return normalized email address
     */
    public String normalizedEmail() {
        return email != null ? email.toLowerCase().trim() : null;
    }
}