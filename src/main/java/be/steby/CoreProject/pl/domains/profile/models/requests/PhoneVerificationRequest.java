package be.steby.CoreProject.pl.domains.profile.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request model for phone number verification.
 * Contains the 6-digit verification code provided by the user.
 *
 * @param verificationCode 6-digit numeric code from SMS
 */
public record PhoneVerificationRequest(

        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "Verification code must be exactly 6 digits")
        @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must contain only digits")
        String verificationCode
) {
}