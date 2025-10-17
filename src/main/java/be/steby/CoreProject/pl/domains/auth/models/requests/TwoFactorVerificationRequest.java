package be.steby.CoreProject.pl.domains.auth.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for two-factor authentication verification
 */
public record TwoFactorVerificationRequest(
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be 6 digits")
    String verificationCode
) {}