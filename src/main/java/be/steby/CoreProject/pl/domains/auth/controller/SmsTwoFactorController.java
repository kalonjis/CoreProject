package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor.SmsTwoFactorService;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for SMS-based Two-Factor Authentication operations.
 * 
 * This controller is specifically responsible for managing SMS 2FA:
 * - Enabling SMS 2FA for authenticated users
 * - Disabling SMS 2FA for authenticated users
 * 
 * Separation of concerns:
 * - This controller handles ONLY SMS 2FA operations
 * - Other 2FA types (EMAIL, TOTP, WebAuthn) have their own controllers
 * - All business logic is delegated to SmsTwoFactorService
 * - Controller focuses solely on HTTP concerns (request/response handling)
 * 
 * Security:
 * - All endpoints require authentication (@PreAuthorize("isAuthenticated()"))
 * - User identity is obtained from SecurityContext (no need to pass user ID)
 * - Input validation is minimal since these are simple enable/disable operations
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/auth/2fa/sms")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class SmsTwoFactorController {
    
    private final SmsTwoFactorService smsTwoFactorService;
    
    /**
     * Enable SMS-based two-factor authentication for the authenticated user.
     * 
     * This endpoint creates a new SMS 2FA configuration and sets it as the
     * primary authentication method. If the user had another 2FA method as primary,
     * it will be demoted to secondary.
     * 
     * SMS 2FA benefits:
     * - Fast delivery (usually within seconds)
     * - Works on any mobile phone (no smartphone required)
     * - Familiar to users (similar to banking SMS codes)
     * - Works offline once code is received
     * 
     * Requirements:
     * - User must have a valid phone number configured
     * - Phone number must be verified and active
     * 
     * Process:
     * 1. Validates that user has a valid phone number
     * 2. Validates that SMS 2FA is not already enabled
     * 3. Demotes any existing primary 2FA method
     * 4. Creates and saves SMS 2FA configuration
     * 5. Sets SMS 2FA as primary method
     * 6. Sends confirmation SMS to user
     * 
     * Response: 200 OK with standardized TwoFactorOperationResponse
     * 
     * Possible errors:
     * - 400 Bad Request: User has no valid phone number configured
     * - 409 Conflict: SMS 2FA already enabled
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/enable")
    public ResponseEntity<TwoFactorOperationResponse> enableSmsTwoFactor() {
        log.info("SMS 2FA enable request received");
        
        // Delegate all business logic to service layer
        smsTwoFactorService.enable();
        
        log.info("SMS 2FA enabled successfully");
        
        // Return standardized response using factory method
        return ResponseEntity.ok(
            TwoFactorOperationResponse.enabled(TwoFactorType.SMS)
        );
    }
    
    /**
     * Disable SMS-based two-factor authentication for the authenticated user.
     * 
     * This endpoint removes the SMS 2FA configuration for the user.
     * The configuration is not deleted but marked as disabled for audit purposes.
     * 
     * Important considerations:
     * - If SMS 2FA was the primary method, no other method is automatically promoted
     * - User will need to explicitly set a new primary method if they have other 2FA methods
     * - If this was the only 2FA method, the user will no longer have 2FA protection
     * 
     * Process:
     * 1. Validates that SMS 2FA is currently enabled
     * 2. Marks the configuration as disabled
     * 3. Removes primary flag if this was the primary method
     * 4. Sets disabled timestamp for audit
     * 
     * Response: 200 OK with standardized TwoFactorOperationResponse
     * 
     * Possible errors:
     * - 404 Not Found: SMS 2FA not enabled for this user
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/disable")
    public ResponseEntity<TwoFactorOperationResponse> disableSmsTwoFactor() {
        log.info("SMS 2FA disable request received");
        
        // Delegate all business logic to service layer
        smsTwoFactorService.disable();
        
        log.info("SMS 2FA disabled successfully");
        
        // Return standardized response using factory method
        return ResponseEntity.ok(
            TwoFactorOperationResponse.disabled(TwoFactorType.SMS)
        );
    }
}