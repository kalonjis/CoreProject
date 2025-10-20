package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * DTO for two-factor authentication method information.
 * Contains user-friendly display information for available 2FA methods.
 *
 * @param type the 2FA method type
 * @param userPublicId the user's publicId
 * @param displayName user-friendly name for the method
 * @param description explanation of how this method works
 * @param isPrimary whether this is the user's primary 2FA method
 * @param isEnabled whether this method is currently enabled
 */
public record TwoFactorAuthDTO(
        String userPublicId,
        TwoFactorType type,
        String displayName,
        String description,
        boolean isPrimary,
        boolean isEnabled
) {
    
    /**
     * Convert TwoFactorAuth entity to DTO with display information.
     * 
     * @param entity the TwoFactorAuth entity to convert
     * @return TwoFactorAuthDTO with user-friendly display data
     */
    public static TwoFactorAuthDTO fromEntity(TwoFactorAuth entity) {
        TwoFactorType type = entity.getType();
        String userPublicId = entity.getUser().getPublicId();
        boolean isPrimary = entity.getIsPrimary();
        boolean isEnabled = entity.getEnabled();
        
        String displayName = switch (type) {
            case TOTP -> "Authenticator App";
            case EMAIL -> "Email Code";
            case SMS -> "SMS Code";
            case BACKUP_CODES -> "Backup Codes";
            case WEBAUTHN -> "Security Key";
        };
        
        String description = switch (type) {
            case TOTP -> "Use your authenticator app to generate a code";
            case EMAIL -> "Send a verification code to your email";
            case SMS -> "Send a verification code to your phone";
            case BACKUP_CODES -> "Use one of your saved backup codes";
            case WEBAUTHN -> "Use your security key or biometric authentication";
        };
        
        return new TwoFactorAuthDTO(
                userPublicId,
                type,
                displayName,
                description,
                isPrimary,
                isEnabled
        );
    }
}