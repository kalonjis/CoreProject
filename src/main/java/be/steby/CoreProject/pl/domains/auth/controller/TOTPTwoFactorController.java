package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.services.twofactor.totptwofactor.TOTPTwoFactorService;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import be.steby.CoreProject.pl.domains.auth.models.responses.TOTPSetupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for TOTP-based Two-Factor Authentication operations.
 * 
 * This controller is specifically responsible for managing TOTP 2FA:
 * - Enabling TOTP 2FA for authenticated users (returns setup info)
 * - Disabling TOTP 2FA for authenticated users
 * 
 * TOTP (Time-based One-Time Password) is the most secure 2FA method:
 * - Works offline (no SMS/email dependency)
 * - Phishing-resistant
 * - Compatible with all major authenticator apps
 * - Follows RFC 6238 standard
 * 
 * Separation of concerns:
 * - This controller handles ONLY TOTP 2FA operations
 * - Other 2FA types (EMAIL, SMS, WebAuthn) have their own controllers
 * - All business logic is delegated to TOTPTwoFactorService
 * - Controller focuses solely on HTTP concerns (request/response handling)
 * 
 * Security:
 * - All endpoints require authentication (@PreAuthorize("isAuthenticated()"))
 * - User identity is obtained from SecurityContext (no need to pass user ID)
 * - Secret keys are handled securely in the service layer
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/auth/2fa/totp")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class TOTPTwoFactorController {
    
    private final TOTPTwoFactorService totpTwoFactorService;
    
    /**
     * Enable TOTP-based two-factor authentication for the authenticated user.
     * 
     * This endpoint creates a new TOTP 2FA configuration and provides setup
     * information for the user to configure their authenticator app. If the user
     * had another 2FA method as primary, it will be demoted to secondary.
     * 
     * TOTP 2FA setup process:
     * 1. Generate a cryptographically secure secret key
     * 2. Create QR code URI for easy app setup
     * 3. Provide manual entry key as backup option
     * 4. Set TOTP as primary 2FA method
     * 
     * The response includes:
     * - QR code URI (for scanning with authenticator app)
     * - Manual entry key (for manual app setup)
     * - Formatted key (for easier manual entry)
     * 
     * Security considerations:
     * - Secret key is generated using cryptographically secure random
     * - Key is encrypted before storage in database
     * - QR code should only be displayed once for security
     * 
     * Compatible authenticator apps:
     * - Google Authenticator
     * - Microsoft Authenticator  
     * - Authy
     * - 1Password
     * - Bitwarden
     * - Any RFC 6238 compliant app
     * 
     * Process:
     * 1. Validates that TOTP 2FA is not already enabled
     * 2. Generates secure secret key and setup information
     * 3. Demotes any existing primary 2FA method
     * 4. Creates and saves TOTP 2FA configuration
     * 5. Sets TOTP 2FA as primary method
     * 
     * Response: 200 OK with TOTPSetupResponse containing setup details
     * 
     * Possible errors:
     * - 409 Conflict: TOTP 2FA already enabled
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing TOTP setup information
     */
    @PostMapping("/enable")
    public ResponseEntity<TOTPSetupResponse> enableTOTPTwoFactor() {
        log.info("TOTP 2FA enable request received");
        
        // Delegate all business logic to service layer
        var setupInfo = totpTwoFactorService.enable();
        
        log.info("TOTP 2FA enabled successfully");
        
        // Return setup information for user to configure authenticator app
        return ResponseEntity.ok(
            TOTPSetupResponse.builder()
                .success(true)
                .message("TOTP two-factor authentication enabled successfully")
                .type(TwoFactorType.TOTP)
                .qrCodeUri(setupInfo.qrCodeUri())
                .manualEntryKey(setupInfo.manualEntryKey())
                .secretKey(setupInfo.secretKey()) // Only return once for security
                .instructions("Scan the QR code with your authenticator app or enter the manual key")
                .build()
        );
    }
    
    /**
     * Disable TOTP-based two-factor authentication for the authenticated user.
     * 
     * This endpoint removes the TOTP 2FA configuration for the user.
     * The configuration is not deleted but marked as disabled for audit purposes.
     * 
     * Important considerations:
     * - If TOTP 2FA was the primary method, no other method is automatically promoted
     * - User will need to explicitly set a new primary method if they have other 2FA methods
     * - If this was the only 2FA method, the user will no longer have 2FA protection
     * - Secret key remains encrypted in database for audit (but marked as disabled)
     * 
     * Security implications:
     * - User should remove the account from their authenticator app
     * - Any backup codes should be regenerated if TOTP was primary method
     * - Consider warning user about reduced account security
     * 
     * Process:
     * 1. Validates that TOTP 2FA is currently enabled
     * 2. Marks the configuration as disabled
     * 3. Removes primary flag if this was the primary method
     * 4. Sets disabled timestamp for audit
     * 
     * Response: 200 OK with standardized TwoFactorOperationResponse
     * 
     * Possible errors:
     * - 404 Not Found: TOTP 2FA not enabled for this user
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/disable")
    public ResponseEntity<TwoFactorOperationResponse> disableTOTPTwoFactor() {
        log.info("TOTP 2FA disable request received");
        
        // Delegate all business logic to service layer
        totpTwoFactorService.disable();
        
        log.info("TOTP 2FA disabled successfully");
        
        // Return standardized response using factory method
        return ResponseEntity.ok(
            TwoFactorOperationResponse.disabled(TwoFactorType.TOTP)
        );
    }


//    @PostMapping("/totp/test-code") TODO: full implementation and use it in front side
//    public ResponseEntity<Boolean> testTotpCode(@RequestBody String code) {
//        boolean isValid = totpTwoFactorService.verifyCode(authenticatedUser, code);
//        return ResponseEntity.ok(isValid);
//    }

}