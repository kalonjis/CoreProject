package be.steby.CoreProject.bll.domains.auth.models;

import java.util.List;

/**
 * Contains backup codes setup result returned when enabling backup codes 2FA.
 * 
 * This record is only returned once during the initial setup.
 * The codes should be displayed to the user immediately and then
 * stored securely by the user.
 * 
 * @param backupCodes List of formatted backup codes (e.g., "ABCD-EFGH-1234-5678")
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record BackupCodesSetupResult(
    List<String> backupCodes
) {
}