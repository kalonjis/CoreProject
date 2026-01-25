
package be.steby.CoreProject.bll.domains.auth.models;

/**
 * BLL request model for SMS 2FA activation verification.
 *
 * Contains the verification code entered by the user and the activation token
 * from the cookie. Used in step 2 of the SMS 2FA setup flow.
 *
 * @param verificationCode The 6-digit verification code entered by user
 * @param activationToken The JWT activation token from HttpOnly cookie
 *
 * @author Steby Team
 * @since 2.0.0
 */
public record SmsTwoFactorActivationBllRequest(
        String verificationCode,
        String activationToken
) {
    /**
     * Validates the request data.
     *
     * @throws IllegalArgumentException if verification code or token is invalid
     */
    public SmsTwoFactorActivationBllRequest {
        if (verificationCode == null || verificationCode.isBlank()) {
            throw new IllegalArgumentException("Verification code is required");
        }
        if (verificationCode.length() != 6) {
            throw new IllegalArgumentException("Verification code must be 6 digits");
        }
        if (!verificationCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("Verification code must contain only digits");
        }
        if (activationToken == null || activationToken.isBlank()) {
            throw new IllegalArgumentException("Activation token is required");
        }
    }
}