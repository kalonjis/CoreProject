package be.steby.CoreProject.pl.domains.account.models.requests;

import be.steby.CoreProject.bll.domains.account.models.SelfSignupRequest;
import be.steby.CoreProject.pl.domains.emailaddress.validators.ValidEmailDomain;
import be.steby.CoreProject.pl.domains.password.validators.StrongPassword;
import jakarta.validation.constraints.*;

/**
 * Minimal signup request - only essential credentials.
 * Profile information (username, firstname, lastname, etc.) is collected separately
 * after account creation via profile completion flow.
 *
 * This follows modern industry patterns where signup is kept minimal to reduce friction,
 * and additional information is collected during onboarding.
 */
public record SignupRequest(
        @NotBlank(message = "Email is required")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        @ValidEmailDomain
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
    /**
     * Custom validation to ensure passwords match.
     */
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     *  Converts PL DTO to BLL DTO (not to entity!)
     */
    public SelfSignupRequest toBllModel() {
        return new SelfSignupRequest(email, password);
    }
}