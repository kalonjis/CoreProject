package be.steby.CoreProject.pl.domains.auth.models.requests;

import be.steby.CoreProject.bll.domains.password.models.VerifySmsPasswordResetBLLRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for SMS password reset code verification.
 * 
 * <p>Contains the 6-digit verification code that the user received via SMS.
 * The JWT token containing the expected code hash is provided via cookie.
 * 
 * <p>Similar to PhoneVerificationRequest but specific to password reset flow.
 * 
 * <p>Usage: POST /api/password/verify-sms-code
 */
public record VerifySmsPasswordResetRequest(
        
        /**
         * The 6-digit verification code sent to the user's phone.
         * Must be exactly 6 numeric characters.
         */
        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "Verification code must be exactly 6 digits")
        @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must contain only digits")
        String verificationCode
) {
    
    /**
     * Converts this PL request to the BLL model.
     * 
     * @param jwtToken the JWT token from the cookie
     * @return BLL model ready for service layer processing
     */
    public VerifySmsPasswordResetBLLRequest toBllModel(String jwtToken) {
        return new VerifySmsPasswordResetBLLRequest(verificationCode, jwtToken);
    }
}