package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.Builder;

/**
 * Response model for TOTP 2FA setup operations.
 * 
 * Contains all information needed for a user to set up TOTP 2FA
 * with their authenticator app of choice.
 * 
 * Security note: The secret key should only be returned once during
 * the setup process for security reasons.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Builder
public record TOTPSetupResponse(
    boolean success,
    String message,
    TwoFactorType type,
    String qrCodeUri,        // URI for QR code generation
    String manualEntryKey,   // Formatted key for manual entry
    String secretKey,        // Base32-encoded secret (return only once)
    String instructions      // Setup instructions for user
) {
    
    /**
     * Factory method for successful TOTP setup.
     */
    public static TOTPSetupResponse success(String qrCodeUri, String manualEntryKey, String secretKey) {
        return TOTPSetupResponse.builder()
            .success(true)
            .message("TOTP two-factor authentication enabled successfully")
            .type(TwoFactorType.TOTP)
            .qrCodeUri(qrCodeUri)
            .manualEntryKey(manualEntryKey)
            .secretKey(secretKey)
            .instructions("Scan the QR code with your authenticator app or enter the manual key")
            .build();
    }
}