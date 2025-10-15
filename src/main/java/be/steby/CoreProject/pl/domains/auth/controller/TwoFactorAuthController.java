package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.services.twofactor.TwoFactorAuthService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Two-Factor Authentication operations.
 * 
 * Handles 2FA setup, verification, and management for authenticated users.
 * All business logic is delegated to TwoFactorAuthService while this controller
 * focuses solely on HTTP concerns (status codes, headers, response formatting).
 * 
 * All endpoints require authentication (user must be logged in).
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class TwoFactorAuthController {
    
    private final TwoFactorAuthService twoFactorAuthService;
    
    /**
     * Enable EMAIL-based two-factor authentication for the authenticated user.
     * 
     * This is the simplest 2FA method:
     * - No external dependencies needed
     * - Uses existing email infrastructure
     * - Good for users without smartphones
     * 
     * Process:
     * 1. Creates 2FA configuration with type=EMAIL
     * 2. Sets it as primary authentication method
     * 3. Sends confirmation email to user
     * 
     * Endpoint: POST /api/auth/2fa/enable-email
     *
     * @return ResponseEntity with success message
     * @throws IllegalStateException if EMAIL 2FA already enabled (handled by global exception handler)
     */
    @PostMapping("/enable-email")
    public ResponseEntity<TwoFactorOperationResponse> enableEmailTwoFactor() {
        
        //log.info("Email 2FA enable request from user: {}", user.getUsername());
        
        // Delegate to service layer
        twoFactorAuthService.enableEmailTwoFactor();
        
       // log.info("Email 2FA enabled successfully for user: {}", user.getUsername());
        
        // Return standardized response
        return ResponseEntity.ok(
            TwoFactorOperationResponse.enabled(TwoFactorType.EMAIL)
        );
    }
}