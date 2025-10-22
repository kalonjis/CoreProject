package be.steby.CoreProject.pl.domains.profile.models.requests.phone;

import be.steby.CoreProject.bll.domains.profile.models.phone.PhoneVerificationRequestBLL;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for phone number verification.
 * Contains the 6-digit verification code and the verification token.
 */
public record PhoneVerificationRequest (

    /**
     * The 6-digit verification code sent to the user's phone.
     * Must be exactly 6 numeric characters.
     */
    @NotBlank(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code must be exactly 6 digits")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must contain only digits")
    String verificationCode
    ) {

    public PhoneVerificationRequestBLL toBLL(String jwtToken){
        return new PhoneVerificationRequestBLL(verificationCode, jwtToken);
    }
}