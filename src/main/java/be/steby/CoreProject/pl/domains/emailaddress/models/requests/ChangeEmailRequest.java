package be.steby.CoreProject.pl.domains.emailaddress.models.requests;

import be.steby.CoreProject.bll.domains.emailaddress.models.EmailChangeRequest;
import be.steby.CoreProject.pl.domains.emailaddress.validators.EmailsMatch;
import be.steby.CoreProject.pl.domains.emailaddress.validators.ValidEmailDomain;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for changing user's email address.
 *
 * <p>This is used when an authenticated user wants to change their email address.
 * The request must include both the new email and a confirmation to prevent typos.
 *
 * <p>Validations:
 * <ul>
 *   <li>Email must be valid format</li>
 *   <li>Email must not exceed 254 characters (RFC 5321)</li>
 *   <li>Email domain must be allowed</li>
 *   <li>Confirmation email must match</li>
 * </ul>
 *
 * <p>Usage: POST /api/email-address/change-request
 */
@EmailsMatch
public record ChangeEmailRequest(
        @NotBlank(message = "Email address cannot be empty")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        @ValidEmailDomain
        String email,

        @NotBlank(message = "Email confirmation cannot be empty")
        @Email(message = "Invalid email format for confirmation")
        @Size(max = 254, message = "Email confirmation cannot exceed 254 characters")
        String confirmEmail
) {
    /**
     * Custom validation to ensure passwords match.
     */
    @AssertTrue(message = "email and  do not match")
    public boolean isEmailsMatch() {
        return email != null && email.equals(confirmEmail);
    }


    /**
     * Converts this PL request model to the ChangeEmailForm expected by the service layer.
     *
     * @return ChangeEmailForm ready for service layer processing
     */
    public EmailChangeRequest toBllModel() {
        return new EmailChangeRequest(email, confirmEmail);
    }
}