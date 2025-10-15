package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Standard response for two-factor authentication operations.
 * Provides consistent response format across all 2FA endpoints.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record TwoFactorOperationResponse(
    String message,
    String type,
    String status
) {
    
    /**
     * Factory method for successful 2FA enable operation.
     */
    public static TwoFactorOperationResponse enabled(TwoFactorType type) {
        return new TwoFactorOperationResponse(
            String.format("%s two-factor authentication has been enabled successfully", 
                         type.getDisplayName()),
            type.name(),
            "enabled"
        );
    }
    
    /**
     * Factory method for successful 2FA disable operation.
     */
    public static TwoFactorOperationResponse disabled(TwoFactorType type) {
        return new TwoFactorOperationResponse(
            String.format("%s two-factor authentication has been disabled", 
                         type.getDisplayName()),
            type.name(),
            "disabled"
        );
    }
    
    /**
     * Factory method for successful 2FA verification.
     */
    public static TwoFactorOperationResponse verified(TwoFactorType type) {
        return new TwoFactorOperationResponse(
            "Two-factor authentication verified successfully",
            type.name(),
            "verified"
        );
    }
}