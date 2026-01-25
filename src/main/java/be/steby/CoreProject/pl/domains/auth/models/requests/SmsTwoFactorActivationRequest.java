
package be.steby.CoreProject.pl.domains.auth.models.requests;

import be.steby.CoreProject.bll.domains.auth.models.SmsTwoFactorActivationBllRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Presentation layer request for SMS 2FA activation verification.
 *
 * Receives the verification code from the request body.
 * The activation token is extracted from the cookie separately.
 *
 * @param verificationCode The 6-digit verification code from SMS
 *
 * @author Steby Team
 * @since 2.0.0
 */
public record SmsTwoFactorActivationRequest(
        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "Verification code must be exactly 6 digits")
        @Pattern(regexp = "\\d{6}", message = "Verification code must contain only digits")
        String verificationCode
) {
    /**
     * Converts this PL request to a BLL request.
     *
     * @param activationToken The activation token from the HttpOnly cookie
     * @return SmsTwoFactorActivationBllRequest for the service layer
     */
    public SmsTwoFactorActivationBllRequest toBll(String activationToken) {
        return new SmsTwoFactorActivationBllRequest(verificationCode, activationToken);
    }
}