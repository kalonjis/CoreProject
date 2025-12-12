package be.steby.CoreProject.bll.domains.auth.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Business logic layer request for email two-factor authentication activation.
 * 
 * This record contains all the necessary data for the business logic layer
 * to process an email 2FA activation request, including both the verification
 * code from the user and the activation token from the secure cookie.
 * 
 * The business logic will:
 * 1. Validate the JWT activation token and extract claims
 * 2. Verify the provided code matches the expected code
 * 3. Activate email 2FA for the user if verification succeeds
 * 
 * @param verificationCode the 6-digit code provided by the user
 * @param activationToken the JWT token containing expected code and user info
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record EmailTwoFactorActivationBllRequest(
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{6}", message = "Verification code must be exactly 6 digits")
    String verificationCode,
    
    @NotBlank(message = "Activation token is required")
    String activationToken
) {
    
    /**
     * Validates the request parameters during record construction.
     * 
     * @throws IllegalArgumentException if any parameter is null, blank, or invalid format
     */
    public EmailTwoFactorActivationBllRequest {
        // Validate verification code
        if (verificationCode == null || verificationCode.isBlank()) {
            throw new IllegalArgumentException("Verification code cannot be null or blank");
        }
        
        // Validate activation token
        if (activationToken == null || activationToken.isBlank()) {
            throw new IllegalArgumentException("Activation token cannot be null or blank");
        }
        
        // Validate verification code format (6 digits)
        String trimmedCode = verificationCode.trim();
        if (!trimmedCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("Verification code must be exactly 6 digits");
        }
    }

    /**
     * Returns the verification code with whitespace trimmed.
     * 
     * @return trimmed verification code
     */
    public String getTrimmedVerificationCode() {
        return verificationCode.trim();
    }

    /**
     * Checks if the verification code appears to be valid format-wise.
     * Note: This only validates format, not correctness against expected code.
     * 
     * @return true if code is 6 digits, false otherwise
     */
    public boolean hasValidCodeFormat() {
        return verificationCode != null && verificationCode.trim().matches("\\d{6}");
    }
}