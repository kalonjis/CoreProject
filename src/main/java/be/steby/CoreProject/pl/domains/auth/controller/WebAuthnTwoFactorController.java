package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.models.webauthn.WebAuthnSetupInfo;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.webauthn.WebAuthnTwoFactorService;
import be.steby.CoreProject.dl.enums.TwoFactorType;
//import be.steby.CoreProject.pl.domains.auth.models.requests.WebAuthnRegistrationCompletionRequest;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import be.steby.CoreProject.pl.domains.auth.models.responses.WebAuthnSetupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for WebAuthn-based Two-Factor Authentication operations.
 * 
 * This controller is specifically responsible for managing WebAuthn 2FA:
 * - Enabling WebAuthn 2FA for authenticated users (2-step process)
 * - Disabling WebAuthn 2FA for authenticated users
 * 
 * WebAuthn Flow:
 * 1. Enable → Returns setup challenge for credential registration
 * 2. Complete → Validates credential and finishes setup
 * 3. During login → TwoFactorFactory routes to this service for authentication
 * 
 * Separation of concerns:
 * - This controller handles ONLY WebAuthn 2FA operations
 * - Other 2FA types (EMAIL, TOTP, SMS) have their own controllers
 * - All business logic is delegated to WebAuthnTwoFactorService
 * - Controller focuses solely on HTTP concerns (request/response handling)
 * 
 * Security:
 * - All endpoints require authentication (@PreAuthorize("isAuthenticated()"))
 * - User identity is obtained from SecurityContext (no need to pass user ID)
 * - WebAuthn challenges are cryptographically secure and time-limited
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/auth/2fa/webauthn")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class WebAuthnTwoFactorController {
    
    private final WebAuthnTwoFactorService webAuthnTwoFactorService;
    
    /**
     * Initiate WebAuthn 2FA activation for the authenticated user.
     * 
     * This is Step 1 of WebAuthn setup. This endpoint initiates the activation
     * process and prepares everything needed for credential registration.
     * 
     * This endpoint:
     * 1. Creates TwoFactorAuth configuration 
     * 2. Generates registration challenge and options
     * 3. Returns setup data for credential creation in browser
     * 
     * WebAuthn 2FA benefits:
     * - Most secure (phishing-resistant)
     * - Convenient (biometric authentication)
     * - No separate device needed (uses device's secure enclave)
     * - Supports various authenticators (fingerprint, Face ID, security keys)
     * 
     * Process:
     * 1. Service validates that WebAuthn 2FA is not already enabled
     * 2. Service creates TwoFactorAuth configuration and WebAuthn entities
     * 3. Service generates challenge and credential creation options
     * 4. Returns setup data for browser credential creation
     * 
     * Response: 200 OK with WebAuthnSetupResponse containing challenge and options
     * 
     * Possible errors:
     * - 409 Conflict: WebAuthn 2FA already enabled
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing WebAuthn setup information
     */
    @PostMapping("/initiate-activation")
    public ResponseEntity<WebAuthnSetupResponse> initiateActivationWebAuthnTwoFactor() {
        log.info("WebAuthn 2FA initiate activation request received");
        
        // Delegate all business logic to service layer
        WebAuthnSetupInfo setupInfo = webAuthnTwoFactorService.initiateActivation();
        
        log.info("WebAuthn 2FA activation initiated successfully");
        
        // Return setup information for browser credential creation
        return ResponseEntity.ok(
            WebAuthnSetupResponse.success(
                setupInfo.challenge(),
                setupInfo.credentialCreationOptions()
            )
        );
    }
    
//    /**
//     * Complete WebAuthn credential registration.
//     *
//     * This is Step 2 of WebAuthn setup. Called after the user has created
//     * a credential using navigator.credentials.create() in their browser.
//     *
//     * This endpoint:
//     * 1. Validates the registration token from enable()
//     * 2. Verifies the credential created by the browser
//     * 3. Stores the credential for future authentication
//     * 4. Completes the WebAuthn 2FA setup
//     *
//     * Process:
//     * 1. Validates registration token (JWT from enable())
//     * 2. Parses and validates WebAuthn credential
//     * 3. Verifies credential against original challenge
//     * 4. Stores credential data for future authentication
//     * 5. Marks WebAuthn 2FA as fully enabled
//     *
//     * Response: 200 OK with standardized TwoFactorOperationResponse
//     *
//     * Possible errors:
//     * - 400 Bad Request: Invalid credential data
//     * - 401 Unauthorized: Invalid or expired registration token
//     * - 422 Unprocessable Entity: Credential verification failed
//     *
//     * @param request containing registration token and WebAuthn credential
//     * @return ResponseEntity containing operation success details
//     */
//    @PostMapping("/complete")
//    public ResponseEntity<TwoFactorOperationResponse> completeWebAuthnRegistration(
//            @Valid @RequestBody WebAuthnRegistrationCompletionRequest request) {
//        log.info("WebAuthn 2FA registration completion request received");
//
//        // Delegate all business logic to service layer
//        boolean success = webAuthnTwoFactorService.completeRegistration(
//            request.registrationToken(),
//            request.webAuthnCredential()
//        );
//
//        if (success) {
//            log.info("WebAuthn 2FA registration completed successfully");
//            return ResponseEntity.ok(
//                TwoFactorOperationResponse.enabled(TwoFactorType.WEBAUTHN)
//            );
//        } else {
//            log.warn("WebAuthn 2FA registration completion failed");
//            return ResponseEntity.unprocessableEntity()
//                .body(TwoFactorOperationResponse.builder()
//                    .success(false)
//                    .message("WebAuthn credential registration failed")
//                    .type(TwoFactorType.WEBAUTHN)
//                    .build());
//        }
//    }
    
    /**
     * Disable WebAuthn-based two-factor authentication for the authenticated user.
     * 
     * This endpoint removes the WebAuthn 2FA configuration and all associated
     * credentials for the user. The configuration is marked as disabled for
     * audit purposes rather than being deleted.
     * 
     * Important considerations:
     * - If WebAuthn 2FA was the primary method, no other method is automatically promoted
     * - User will need to explicitly set a new primary method if they have other 2FA methods
     * - If this was the only 2FA method, the user will no longer have 2FA protection
     * - All WebAuthn credentials are removed from the device/browser
     * 
     * Security implications:
     * - User should be informed that their biometric/security key access is revoked
     * - Consider warning user about reduced account security
     * - All stored credentials are securely deleted
     * 
     * Process:
     * 1. Validates that WebAuthn 2FA is currently enabled
     * 2. Removes all associated WebAuthn credentials
     * 3. Marks the configuration as disabled
     * 4. Removes primary flag if this was the primary method
     * 5. Sets disabled timestamp for audit
     * 
     * Response: 200 OK with standardized TwoFactorOperationResponse
     * 
     * Possible errors:
     * - 404 Not Found: WebAuthn 2FA not enabled for this user
     * - 401 Unauthorized: User not authenticated
     * 
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/disable")
    public ResponseEntity<TwoFactorOperationResponse> disableWebAuthnTwoFactor() {
        log.info("WebAuthn 2FA disable request received");
        
        // Delegate all business logic to service layer
        webAuthnTwoFactorService.disable();
        
        log.info("WebAuthn 2FA disabled successfully");
        
        // Return standardized response using factory method
        return ResponseEntity.ok(
            TwoFactorOperationResponse.disabled(TwoFactorType.WEBAUTHN)
        );
    }
}