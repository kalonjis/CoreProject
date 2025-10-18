package be.steby.CoreProject.pl.domains.auth.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for TOTP code verification.
 */
public record VerifyTOTPRequest(
    
    /**
     * 6-digit TOTP code from authenticator app
     */
    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be exactly 6 digits")
    String code
) {}