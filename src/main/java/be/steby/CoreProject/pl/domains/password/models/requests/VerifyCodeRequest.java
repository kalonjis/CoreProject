package be.steby.CoreProject.pl.domains.password.models.requests;

import be.steby.CoreProject.bll.domains.password.models.VerifyCodeBLLRequest;
import be.steby.CoreProject.dl.enums.PasswordResetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for password reset code verification.
 *
 * <p>Contains the 6-digit verification code that the user received via email or SMS.
 * The JWT token containing the expected code hash reference is provided via cookie.
 *
 * <p><strong>Usage:</strong> Used for both {@link PasswordResetType#EMAIL_CODE}
 * and {@link PasswordResetType#SMS_CODE} verification flows.
 *
 * <p><strong>Endpoint:</strong> POST /api/password/verify-code
 *
 * @see VerifyCodeBLLRequest
 */
public record VerifyCodeRequest(

        /**
         * The 6-digit verification code sent to the user's email or phone.
         *
         * <p>Must be exactly 6 numeric characters. This constraint is enforced
         * by validation annotations and provides immediate feedback to the user.
         */
        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "Verification code must be exactly 6 digits")
        @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must contain only digits")
        String verificationCode

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * <p>Combines the user-provided verification code with the JWT token
     * extracted from the cookie by the controller.
     *
     * @param jwtToken the JWT token from the verification cookie
     * @return BLL model ready for service layer processing
     */
    public VerifyCodeBLLRequest toBllModel(String jwtToken) {
        return new VerifyCodeBLLRequest(verificationCode, jwtToken);
    }
}