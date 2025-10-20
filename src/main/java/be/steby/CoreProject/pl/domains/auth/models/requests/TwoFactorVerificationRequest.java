package be.steby.CoreProject.pl.domains.auth.models.requests;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/**
 * Request DTO for two-factor authentication verification
 * Supports both 6-digit codes and backup codes with separate properties
 */
public record TwoFactorVerificationRequest(
        @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be 6 digits")
        String verificationCode,

        @Pattern(regexp = "^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$",
                message = "Backup code must be in format XXXX-XXXX-XXXX-XXXX")
        String backupCode
) {

    /**
     * Factory method for numeric verification codes (EMAIL, SMS, TOTP)
     */
    public static TwoFactorVerificationRequest forDigits(String verificationCode) {
        return new TwoFactorVerificationRequest(verificationCode, null);
    }

    /**
     * Factory method for backup codes
     */
    public static TwoFactorVerificationRequest forBackupCode(String backupCode) {
        return new TwoFactorVerificationRequest(null, backupCode);
    }

    /**
     * Compact constructor with validation
     */
    public TwoFactorVerificationRequest {
        // Validate that exactly one of the two properties is provided
        if ((verificationCode == null && backupCode == null)) {
            throw new IllegalArgumentException("Either verificationCode or backupCode must be provided");
        }
        if (verificationCode != null && backupCode != null) {
            throw new IllegalArgumentException("Only one of verificationCode or backupCode can be provided, not both");
        }
    }

    /**
     * Determines if this request contains a numeric verification code
     */
    public boolean hasVerificationCode() {
        return verificationCode != null && !verificationCode.trim().isEmpty();
    }

    /**
     * Determines if this request contains a backup code
     */
    public boolean hasBackupCode() {
        return backupCode != null && !backupCode.trim().isEmpty();
    }

    /**
     * Gets the actual code value regardless of type
     */
    public String getCodeValue() {
        return hasVerificationCode() ? verificationCode : backupCode;
    }
}