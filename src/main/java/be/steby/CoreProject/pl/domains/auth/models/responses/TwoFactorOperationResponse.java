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
     * Creates a successful activation initiation response.
     *
     * @param type the type of 2FA method being activated
     * @return TwoFactorOperationResponse indicating successful initiation
     */
    public static TwoFactorOperationResponse initiateActivationSuccess(TwoFactorType type) {
        return new TwoFactorOperationResponse(
            String.format("%s two-factor authentication setup initiated successfully. Please check your %s for the verification code.",
                    type.getDisplayName(),
                    type == TwoFactorType.EMAIL ? "email" : (
                            type == TwoFactorType.TOTP? "authenticator app" : "sms"
                    )
            ),
            type.name(),
            String.format("%s two-factor activation initiated", type.getDisplayName())
        );
    }

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


    /**
     * Success response for 2FA method selection.
     */
    public static TwoFactorOperationResponse methodChosen(TwoFactorType chosenMethod) {
        return new TwoFactorOperationResponse(
                "2FA_METHOD_CHOSEN",
                chosenMethod.name(),
                "Two-factor method selected successfully. " + getMethodMessage(chosenMethod)
        );
    }

    private static String getMethodMessage(TwoFactorType type) {
        return switch (type) {
            case EMAIL -> "Verification code sent to your email.";
            case TOTP -> "Enter the code from your authenticator app.";
            case SMS -> "Verification code sent to your phone.";
            case BACKUP_CODES -> "Enter one of your saved backup codes.";
            case WEBAUTHN -> "Use your security key to authenticate.";
        };
    }
}