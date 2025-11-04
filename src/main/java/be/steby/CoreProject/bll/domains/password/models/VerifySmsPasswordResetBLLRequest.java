package be.steby.CoreProject.bll.domains.password.models;

/**
 * Business layer DTO for SMS password reset code verification.
 * 
 * <p>Contains the user-provided verification code and the JWT token from cookie
 * that needs to be validated. Clean data transfer object for business logic processing.
 */
public record VerifySmsPasswordResetBLLRequest(
        /**
         * The 6-digit verification code provided by the user.
         * Already validated by presentation layer for format and length.
         */
        String verificationCode,
        
        /**
         * The JWT token from the password reset SMS cookie.
         * Contains the hashed expected code and user email for validation.
         */
        String jwtToken
) {
    // Pure data transfer object - business validation in service layer
}