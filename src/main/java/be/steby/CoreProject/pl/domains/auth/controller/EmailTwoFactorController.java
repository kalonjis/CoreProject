package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor.EmailTwoFactorService;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Email-based Two-Factor Authentication operations.
 * 
 * This controller is specifically responsible for managing email 2FA:
 * - Enabling email 2FA for authenticated users
 * - Disabling email 2FA for authenticated users
 * 
 * Separation of concerns:
 * - This controller handles ONLY email 2FA operations
 * - Other 2FA types (TOTP, SMS, WebAuthn) will have their own controllers
 * - All business logic is delegated to EmailTwoFactorService
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
@RequestMapping("/api/auth/2fa/email")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class EmailTwoFactorController {
    
    private final EmailTwoFactorService emailTwoFactorService;
    
    /**
     * Enable email-based two-factor authentication for the authenticated user.
     * 
     * This endpoint creates a new email 2FA configuration and sets it as the
     * primary authentication method. If the user had another 2FA method as primary,
     * it will be demoted to secondary.
     * 
     * Email 2FA benefits:
     * - Uses existing email infrastructure (no external dependencies)
     * - Works on any device with email access
     * - Familiar to users (similar to password reset flow)
     * - No smartphone required
     * 
     * Process:
     * 1. Validates that email 2FA is not already enabled
     * 2. Demotes any existing primary 2FA method
     * 3. Creates and saves email 2FA configuration
     * 4. Sets email 2FA as primary method
     * 
     * Response: 200 OK with standardized TwoFactorOperationResponse
     * 
     * Possible errors:
     * - 409 Conflict: Email 2FA already enabled
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/enable")
    public ResponseEntity<TwoFactorOperationResponse> enableEmailTwoFactor() {
        log.info("Email 2FA enable request received");
        
        // Delegate all business logic to service layer
        emailTwoFactorService.enable();
        
        log.info("Email 2FA enabled successfully");
        
        // Return standardized response using factory method
        return ResponseEntity.ok(
            TwoFactorOperationResponse.enabled(TwoFactorType.EMAIL)
        );
    }
    
    /**
     * Disable email-based two-factor authentication for the authenticated user.
     * 
     * This endpoint removes the email 2FA configuration for the user.
     * The configuration is not deleted but marked as disabled for audit purposes.
     * 
     * Important considerations:
     * - If email 2FA was the primary method, no other method is automatically promoted
     * - User will need to explicitly set a new primary method if they have other 2FA methods
     * - If this was the only 2FA method, the user will no longer have 2FA protection
     * 
     * Process:
     * 1. Validates that email 2FA is currently enabled
     * 2. Marks the configuration as disabled
     * 3. Removes primary flag if this was the primary method
     * 4. Sets disabled timestamp for audit
     * 
     * Response: 200 OK with standardized TwoFactorOperationResponse
     * 
     * Possible errors:
     * - 404 Not Found: Email 2FA not enabled for this user
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/disable")
    public ResponseEntity<TwoFactorOperationResponse> disableEmailTwoFactor() {
        log.info("Email 2FA disable request received");
        
        // Delegate all business logic to service layer
        emailTwoFactorService.disable();
        
        log.info("Email 2FA disabled successfully");
        
        // Return standardized response using factory method
        return ResponseEntity.ok(
            TwoFactorOperationResponse.disabled(TwoFactorType.EMAIL)
        );
    }
}