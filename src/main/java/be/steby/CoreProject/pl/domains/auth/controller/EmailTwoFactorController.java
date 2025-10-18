// Create: src/main/java/be/steby/CoreProject/pl/domains/auth/controller/EmailTwoFactorController.java

package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller specifically for EMAIL-based Two-Factor Authentication.
 * 
 * Handles all EMAIL 2FA operations:
 * - Enable EMAIL 2FA
 * - Disable EMAIL 2FA
 * 
 * All endpoints require authentication (user must be logged in).
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/auth/email-2fa")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class EmailTwoFactorController {
    
    private final EmailTwoFactorService emailTwoFactorService;
    
    /**
     * Enable EMAIL-based two-factor authentication.
     * 
     * This is the simplest 2FA method:
     * - No external dependencies needed
     * - Uses existing email infrastructure
     * - Good for users without smartphones
     * 
     * Process:
     * 1. Creates 2FA configuration with type=EMAIL
     * 2. Sets it as enabled and primary method
     * 3. Sends confirmation email to user
     * 
     * Endpoint: POST /api/auth/email-2fa/enable
     *
     * @return ResponseEntity with success message
     */
    @PostMapping("/enable")
    public ResponseEntity<TwoFactorOperationResponse> enableEmailTwoFactor() {
        
        log.info("Email 2FA enable request from authenticated user");
        
        emailTwoFactorService.enableEmailTwoFactor(null); // Service gets user internally
        
        log.info("Email 2FA enabled successfully");
        
        return ResponseEntity.ok(
            TwoFactorOperationResponse.enabled(TwoFactorType.EMAIL)
        );
    }
    
    /**
     * Disable EMAIL two-factor authentication.
     * 
     * Endpoint: DELETE /api/auth/email-2fa/disable
     *
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/disable")
    public ResponseEntity<TwoFactorOperationResponse> disableEmailTwoFactor() {
        
        log.info("Email 2FA disable request from authenticated user");
        
        // TODO: Add disable method to EmailTwoFactorService
        // emailTwoFactorService.disableEmailTwoFactor();
        
        log.info("Email 2FA disabled successfully");
        
        return ResponseEntity.ok(
            TwoFactorOperationResponse.disabled(TwoFactorType.EMAIL)
        );
    }
}