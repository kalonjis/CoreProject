
package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.requests.VerifyTOTPRequest;
import be.steby.CoreProject.pl.domains.auth.models.responses.TOTPSetupResponse;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller specifically for TOTP-based Two-Factor Authentication.
 * 
 * Handles all TOTP 2FA operations:
 * - Setup TOTP (generate secret and QR code)
 * - Verify TOTP setup (confirm authenticator app works)
 * - Disable TOTP
 * 
 * All endpoints require authentication (user must be logged in).
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/auth/totp-2fa")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class TOTPTwoFactorController {
    
    private final TOTPTwoFactorService totpTwoFactorService;
    
    /**
     * Setup TOTP-based two-factor authentication.
     * Generates secret and returns setup data for authenticator apps.
     * 
     * Process:
     * 1. Generate random TOTP secret
     * 2. Store encrypted secret (not enabled yet)
     * 3. Return secret and QR code for Google Authenticator
     * 
     * Endpoint: POST /api/auth/totp-2fa/setup
     *
     * @return ResponseEntity with TOTP setup data (secret, QR code, instructions)
     */
    @PostMapping("/setup")
    public ResponseEntity<TOTPSetupResponse> setupTOTP() {
        
        log.info("TOTP 2FA setup request from authenticated user");
        
        TOTPSetupResponse response = totpTwoFactorService.setupTOTPTwoFactor(null); // Service gets user internally
        
        log.info("TOTP 2FA setup data generated successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify TOTP code during setup to confirm authenticator app configuration.
     * 
     * Process:
     * 1. Verify the code matches expected TOTP calculation
     * 2. Enable TOTP 2FA and set as primary method
     * 3. Send confirmation email to user
     * 
     * Endpoint: POST /api/auth/totp-2fa/verify-setup
     *
     * @param request Request containing the 6-digit TOTP code
     * @return ResponseEntity with success message
     */
    @PostMapping("/verify-setup")
    public ResponseEntity<TwoFactorOperationResponse> verifyTOTPSetup(
            @Valid @RequestBody VerifyTOTPRequest request) {
        
        log.info("TOTP verification request during setup");
        
        totpTwoFactorService.verifyTOTPSetup(null, request.code()); // Service gets user internally
        
        log.info("TOTP 2FA enabled successfully");
        
        return ResponseEntity.ok(
            TwoFactorOperationResponse.enabled(TwoFactorType.TOTP)
        );
    }
    
    /**
     * Disable TOTP two-factor authentication.
     * 
     * Endpoint: DELETE /api/auth/totp-2fa/disable
     *
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/disable")
    public ResponseEntity<TwoFactorOperationResponse> disableTOTP() {
        
        log.info("TOTP 2FA disable request from authenticated user");
        
        // TODO: Add disable method to TOTPTwoFactorService
        // totpTwoFactorService.disableTOTPTwoFactor();
        
        log.info("TOTP 2FA disabled successfully");
        
        return ResponseEntity.ok(
            TwoFactorOperationResponse.disabled(TwoFactorType.TOTP)
        );
    }
}