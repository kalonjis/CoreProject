package be.steby.CoreProject.pl.domains.auth.models.requests;

import be.steby.CoreProject.bll.domains.auth.models.EmailTwoFactorActivationBllRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Presentation layer request for email two-factor authentication activation.
 * 
 * This record represents the HTTP request body sent by the frontend when
 * a user submits their verification code to activate email 2FA. It contains
 * only the verification code, as the activation token is passed via cookie.
 * 
 * The request is validated at the controller level before being converted
 * to the corresponding BLL request for business logic processing.
 * 
 * @param verificationCode the 6-digit verification code entered by the user
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record EmailTwoFactorActivationRequest(
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{6}", message = "Verification code must be exactly 6 digits")
    String verificationCode
) {
    
    /**
     * Validates the request parameters during record construction.
     * 
     * @throws IllegalArgumentException if verification code is null, blank, or invalid format
     */
    public EmailTwoFactorActivationRequest {
        if (verificationCode == null || verificationCode.isBlank()) {
            throw new IllegalArgumentException("Verification code cannot be null or blank");
        }
        
        // Remove whitespace for validation
        String trimmed = verificationCode.trim();
        if (!trimmed.matches("\\d{6}")) {
            throw new IllegalArgumentException("Verification code must be exactly 6 digits");
        }
    }

    /**
     * Converts this PL request to the corresponding BLL request.
     * 
     * @param activationToken the JWT activation token from the cookie
     * @return EmailTwoFactorActivationBllRequest for business logic processing
     * @throws IllegalArgumentException if activationToken is null or blank
     */
    public EmailTwoFactorActivationBllRequest toBll(String activationToken) {
        if (activationToken == null || activationToken.isBlank()) {
            throw new IllegalArgumentException("Activation token is required");
        }
        
        return new EmailTwoFactorActivationBllRequest(
            verificationCode.trim(), 
            activationToken
        );
    }
}