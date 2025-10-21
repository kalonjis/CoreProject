package be.steby.CoreProject.pl.domains.profile.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response model for phone verification operations.
 * 
 * @param success Whether the operation was successful
 * @param message Human-readable message about the operation result
 * @param maskedPhoneNumber Masked phone number for display (only for verification requests)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhoneVerificationResponse(
        boolean success,
        String message,
        String maskedPhoneNumber
) {

    /**
     * Factory method for successful verification request.
     * Used when verification SMS is sent successfully.
     */
    public static PhoneVerificationResponse verificationSent(String maskedPhoneNumber) {
        return new PhoneVerificationResponse(
                true,
                "Verification code sent successfully",
                maskedPhoneNumber
        );
    }

    /**
     * Factory method for successful code verification.
     * Used when the provided code is valid and phone is marked as verified.
     */
    public static PhoneVerificationResponse verificationSuccessful() {
        return new PhoneVerificationResponse(
                true,
                "Phone number verified successfully",
                null
        );
    }

    /**
     * Factory method for failed code verification.
     * Used when the provided code is invalid.
     */
    public static PhoneVerificationResponse verificationFailed(String reason) {
        return new PhoneVerificationResponse(
                false,
                reason,
                null
        );
    }
}