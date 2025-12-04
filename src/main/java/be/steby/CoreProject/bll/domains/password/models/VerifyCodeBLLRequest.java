package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Business layer DTO for password reset code verification.
 *
 * <p>Contains the user-provided verification code and the JWT token from cookie
 * that needs to be validated. This is a clean data transfer object for business
 * logic processing.
 *
 * <p><strong>Usage:</strong> Used for both {@link PasswordResetType#EMAIL_CODE}
 * and {@link PasswordResetType#SMS_CODE} verification flows.
 *
 * <p><strong>Validation:</strong>
 * <ul>
 *   <li>Presentation layer validates format (6 digits)</li>
 *   <li>Business layer validates against stored hash</li>
 * </ul>
 *
 * @param verificationCode the 6-digit verification code provided by the user
 * @param jwtToken the JWT token from the verification cookie
 *
 * @see CodeVerificationResult
 */
public record VerifyCodeBLLRequest(

        /**
         * The 6-digit verification code provided by the user.
         *
         * <p>Already validated by presentation layer for format and length.
         * Must be exactly 6 numeric characters.
         */
        String verificationCode,

        /**
         * The JWT token from the password reset verification cookie.
         *
         * <p>Contains a reference to the database token where the hashed
         * expected code is stored. This lightweight approach keeps the
         * cookie small while maintaining security.
         */
        String jwtToken

) {
    /**
     * Checks if this request has all required data.
     *
     * <p>Convenience method for quick validation before processing.
     *
     * @return true if both verification code and JWT token are present and non-blank
     */
    public boolean isComplete() {
        return verificationCode != null && !verificationCode.isBlank()
                && jwtToken != null && !jwtToken.isBlank();
    }
}