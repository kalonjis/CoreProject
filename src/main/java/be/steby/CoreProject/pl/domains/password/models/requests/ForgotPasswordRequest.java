package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.bll.domains.password.models.ForgotPasswordBLLRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for initiating a password reset.
 *
 * <p>The reset type is no longer part of the request body — it is determined
 * by the endpoint itself (/forgot/email-link, /forgot/email-code, /forgot/sms-code).
 *
 * <p>Usage:
 * <pre>
 *   POST /api/password/forgot/email-link
 *   POST /api/password/forgot/email-code
 *   POST /api/password/forgot/sms-code
 * </pre>
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email address is required")
        @Size(max = 254, message = "Email address cannot exceed 254 characters")
        @Email(message = "Email address must be valid")
        String email

) {
    public String normalizedEmail() {
        return email != null ? email.toLowerCase().trim() : null;
    }

    public ForgotPasswordBLLRequest toBllModel() {
        return new ForgotPasswordBLLRequest(normalizedEmail());
    }
}