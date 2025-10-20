package be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodetwofactor;

import be.steby.CoreProject.dl.entities.User;

import java.util.List;

/**
 * Service interface for managing backup code 2FA authentication.
 *
 * Backup codes serve as a recovery mechanism when users lose access to their primary 2FA method.
 * They are one-time use codes that should be stored securely by the user.
 *
 * Key characteristics:
 * - Generated once when 2FA is first enabled
 * - Each code can only be used once
 * - Should be human-readable and easy to transcribe
 * - Stored hashed in database for security
 * - Used only for account recovery, not regular login
 *
 * Typical usage flow:
 * 1. User enables 2FA → backup codes are generated and displayed once
 * 2. User saves codes securely (print, password manager, etc.)
 * 3. User loses primary 2FA device
 * 4. User uses backup code to login and can re-configure 2FA
 * 5. Used backup code is consumed and can't be reused
 *
 * @author Steby Team
 * @since 2.0.0
 */
public interface BackupCodeTwoFactorService {

    /**
     * Generate a new set of backup codes for the user.
     * 
     * This will replace any existing backup codes and reset the count.
     * Should only be called when initially setting up 2FA or when user 
     * explicitly requests new backup codes.
     *
     * @param user the user for whom to generate backup codes
     * @return List of plain text backup codes (shown only once)
     * @throws IllegalStateException if user doesn't have any other 2FA method enabled
     */
    List<String> generateBackupCodes(User user);

    /**
     * Verify a backup code provided by the user.
     * 
     * This will consume the code if valid (mark it as used).
     * Should record failed attempts for security.
     *
     * @param user the user attempting to use the backup code
     * @param providedCode the backup code provided by the user
     * @return true if the code is valid and unused, false otherwise
     */
    boolean verifyAndConsumeBackupCode(User user, String providedCode);

    /**
     * Get the number of remaining (unused) backup codes for the user.
     *
     * @param user the user to check
     * @return number of remaining backup codes, 0 if none available
     */
    int getRemainingBackupCodesCount(User user);

    /**
     * Check if the user has backup codes available.
     *
     * @param user the user to check
     * @return true if user has at least one unused backup code
     */
    boolean hasBackupCodesAvailable(User user);

    /**
     * Regenerate backup codes (invalidate old ones and create new ones).
     * 
     * This is typically used when user wants fresh backup codes or 
     * suspects their existing codes may have been compromised.
     *
     * @param user the user for whom to regenerate backup codes
     * @return List of new plain text backup codes
     * @throws IllegalStateException if user doesn't have any other 2FA method enabled
     */
    List<String> regenerateBackupCodes(User user);

    /**
     * Disable backup codes for the user (typically when disabling all 2FA).
     * 
     * This will mark all backup codes as unusable but keep them for audit purposes.
     *
     * @param user the user for whom to disable backup codes
     */
    void disableBackupCodes(User user);

    /**
     * Check if backup codes are enabled for the user.
     *
     * @param user the user to check
     * @return true if backup codes are enabled
     */
    boolean isBackupCodesEnabled(User user);
}