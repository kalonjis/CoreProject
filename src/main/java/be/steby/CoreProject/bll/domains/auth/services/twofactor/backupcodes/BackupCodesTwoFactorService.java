package be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodes;

import be.steby.CoreProject.bll.domains.auth.models.BackupCodesSetupResult;
import be.steby.CoreProject.dl.entities.User;

/**
 * Service interface for Backup Codes Two-Factor Authentication operations.
 *
 * Handles the setup, management, and verification of backup codes 2FA.
 * Backup codes are single-use recovery codes that can be used when primary
 * 2FA methods are unavailable (e.g., lost phone, broken authenticator app).
 *
 * Backup codes 2FA flow:
 * 1. User enables backup codes 2FA -> codes generated and displayed once
 * 2. User saves codes in a secure location
 * 3. During login (when primary 2FA unavailable) -> user enters one backup code
 * 4. Code verified and marked as used, login completed
 *
 * @author Steby Team
 * @since 2.0.0
 */
public interface BackupCodesTwoFactorService {

    /**
     * Enable backup codes 2FA for the authenticated user.
     *
     * Generates a new set of backup codes and stores them encrypted in the database.
     * Each code can only be used once. This method should only be called when
     * displaying the codes to the user for the first time.
     *
     * @return BackupCodesSetupResult containing the generated codes for display
     */
    BackupCodesSetupResult enable();

    /**
     * Disable backup codes 2FA for the authenticated user.
     *
     * Marks all backup codes as disabled. The codes are not deleted for audit purposes.
     */
    void disable();

    /**
     * Generate a verification code for backup codes 2FA.
     *
     * Note: For backup codes, this method doesn't generate new codes.
     * Instead, it's used during the verification process to validate
     * that a provided code exists and hasn't been used.
     *
     * @param user the user for backup code verification
     * @return empty string (backup codes don't generate dynamic codes)
     */
    String generateCode(User user);

    /**
     * Verify a provided backup code against stored codes.
     *
     * Checks if the provided code exists, hasn't been used, and marks it as used
     * if verification is successful. Uses constant-time comparison to prevent
     * timing attacks.
     *
     * @param user the user attempting verification
     * @param providedCode the backup code entered by the user
     * @return true if the code is valid and unused, false otherwise
     */
    boolean verifyCode(User user, String providedCode);
}