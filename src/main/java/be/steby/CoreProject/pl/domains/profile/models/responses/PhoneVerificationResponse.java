package be.steby.CoreProject.pl.domains.profile.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response model for SMS verification operations.
 * 
 * @param message Human-readable message about the operation result
 */

public record PhoneVerificationResponse(
        String message
) {

    /**
     * Factory method for successful verification request.
     * Used when verification SMS is sent successfully.
     */
    public static PhoneVerificationResponse verificationSent() {
        return new PhoneVerificationResponse(
                "Verification code sent successfully"
        );
    }

    /**
     * Factory method for successful code verification.
     * Used when the provided code is valid and phone is marked as verified.
     */
    public static PhoneVerificationResponse verificationSuccessful() {
        return new PhoneVerificationResponse(
                "Phone number verified successfully"
        );
    }
}