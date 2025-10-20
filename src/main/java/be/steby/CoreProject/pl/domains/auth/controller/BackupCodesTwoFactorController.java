package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodes.BackupCodesTwoFactorService;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import be.steby.CoreProject.pl.domains.auth.models.responses.BackupCodesSetupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Backup Codes Two-Factor Authentication operations.
 * 
 * This controller is specifically responsible for managing backup codes 2FA:
 * - Enabling backup codes 2FA for authenticated users (returns backup codes)
 * - Disabling backup codes 2FA for authenticated users
 * 
 * Backup codes 2FA is used as a recovery method:
 * - Single-use codes for account recovery
 * - Used when primary 2FA methods are unavailable
 * - Each code can only be used once
 * - Codes should be stored securely by the user
 * 
 * Separation of concerns:
 * - This controller handles ONLY backup codes 2FA operations
 * - Other 2FA types (EMAIL, TOTP, SMS, WebAuthn) have their own controllers
 * - All business logic is delegated to BackupCodesTwoFactorService
 * - Controller focuses solely on HTTP concerns (request/response handling)
 * 
 * Security:
 * - All endpoints require authentication (@PreAuthorize("isAuthenticated()"))
 * - User identity is obtained from SecurityContext (no need to pass user ID)
 * - Backup codes are only shown once during setup for security
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/auth/2fa/backup-codes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class BackupCodesTwoFactorController {
    
    private final BackupCodesTwoFactorService backupCodesTwoFactorService;
    
    /**
     * Enable backup codes two-factor authentication for the authenticated user.
     * 
     * This endpoint creates a new backup codes 2FA configuration and generates
     * a set of recovery codes for the user. If the user had another 2FA method
     * as primary, it will be demoted to secondary.
     * 
     * Backup codes 2FA setup process:
     * 1. Generate 10 cryptographically secure backup codes
     * 2. Hash each code with BCrypt for secure storage
     * 3. Create 2FA configuration and set as primary
     * 4. Return codes to user (ONLY shown once)
     * 
     * The response includes:
     * - List of 10 backup codes (formatted as ABCD-EFGH-1234-5678)
     * - Instructions for secure storage
     * 
     * Security considerations:
     * - Codes are generated using cryptographically secure random
     * - Codes are hashed with BCrypt before storage (irreversible)
     * - Codes are only returned once for security
     * - Each code can only be used once (single-use)
     * 
     * Process:
     * 1. Validates that backup codes 2FA is not already enabled
     * 2. Generates 10 secure backup codes
     * 3. Demotes any existing primary 2FA method
     * 4. Creates and saves backup codes 2FA configuration
     * 5. Sets backup codes 2FA as primary method
     * 
     * Response: 200 OK with BackupCodesSetupResponse containing codes
     * 
     * Possible errors:
     * - 409 Conflict: Backup codes 2FA already enabled
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing backup codes setup information
     */
    @PostMapping("/enable")
    public ResponseEntity<BackupCodesSetupResponse> enableBackupCodesTwoFactor() {
        log.info("Backup codes 2FA enable request received");
        
        // Delegate all business logic to service layer
        var setupResult = backupCodesTwoFactorService.enable();
        
        log.info("Backup codes 2FA enabled successfully");
        
        // Return setup information with backup codes
        return ResponseEntity.ok(
            BackupCodesSetupResponse.success(setupResult.backupCodes())
        );
    }
    
    /**
     * Disable backup codes two-factor authentication for the authenticated user.
     * 
     * This endpoint removes the backup codes 2FA configuration for the user.
     * The configuration is not deleted but marked as disabled for audit purposes.
     * 
     * Important considerations:
     * - If backup codes 2FA was the primary method, no other method is automatically promoted
     * - User will need to explicitly set a new primary method if they have other 2FA methods
     * - If this was the only 2FA method, the user will no longer have 2FA protection
     * - Backup codes remain hashed in database for audit (but marked as disabled)
     * 
     * Security implications:
     * - User should securely destroy any stored backup codes
     * - Consider warning user about reduced account security
     * - Generate new backup codes if re-enabling later
     * 
     * Process:
     * 1. Validates that backup codes 2FA is currently enabled
     * 2. Marks the configuration as disabled
     * 3. Removes primary flag if this was the primary method
     * 4. Sets disabled timestamp for audit
     * 
     * Response: 200 OK with standardized TwoFactorOperationResponse
     * 
     * Possible errors:
     * - 404 Not Found: Backup codes 2FA not enabled for this user
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/disable")
    public ResponseEntity<TwoFactorOperationResponse> disableBackupCodesTwoFactor() {
        log.info("Backup codes 2FA disable request received");
        
        // Delegate all business logic to service layer
        backupCodesTwoFactorService.disable();
        
        log.info("Backup codes 2FA disabled successfully");
        
        // Return standardized response using factory method
        return ResponseEntity.ok(
            TwoFactorOperationResponse.disabled(TwoFactorType.BACKUP_CODES)
        );
    }
}