package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.Builder;

import java.util.List;

/**
 * Response model for backup codes 2FA setup operations.
 * 
 * Contains all backup codes generated for the user. These codes should only
 * be shown once during the initial setup for security reasons.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Builder
public record BackupCodesSetupResponse(
    boolean success,
    String message,
    TwoFactorType type,
    List<String> backupCodes,
    String instructions
) {
    
    /**
     * Factory method for successful backup codes setup.
     */
    public static BackupCodesSetupResponse success(List<String> backupCodes) {
        return BackupCodesSetupResponse.builder()
            .success(true)
            .message("Backup codes two-factor authentication enabled successfully")
            .type(TwoFactorType.BACKUP_CODES)
            .backupCodes(backupCodes)
            .instructions("Save these backup codes in a secure location. Each code can only be used once.")
            .build();
    }
}