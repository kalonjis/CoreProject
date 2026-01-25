package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.models.TwoFactorActivationResult;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor.SmsTwoFactorService;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.requests.SmsTwoFactorActivationRequest;
import be.steby.CoreProject.pl.domains.auth.models.requests.TwoFactorVerificationRequest;
import be.steby.CoreProject.pl.domains.auth.models.responses.TwoFactorOperationResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    private final AuthCookieService authCookieService;
    
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


    /**
     * Initiate SMS two-factor authentication activation process.
     *
     * This endpoint starts the setup process for SMS-based 2FA by:
     * 1. Validating that SMS 2FA is not already enabled
     * 2. Validating that user has a verified phone number
     * 3. Generating a verification code and activation token
     * 4. Publishing an event to send the verification SMS
     * 5. Setting the activation token as an HttpOnly cookie
     *
     * The activation token is a JWT containing:
     * - User's public ID
     * - Hashed verification code
     * - Expiration time (typically 10 minutes)
     *
     * Response: 200 OK with standardized TwoFactorOperationResponse
     *
     * Possible errors:
     * - 400 Bad Request: User has no verified phone number
     * - 409 Conflict: SMS 2FA already enabled
     * - 429 Too Many Requests: Rate limit exceeded
     * - 401 Unauthorized: User not authenticated
     *
     * @param httpResponse HTTP response for setting the activation cookie
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/setup/initiate")
    public ResponseEntity<TwoFactorOperationResponse> initiateSmsSetup(
            HttpServletResponse httpResponse
    ) {
        log.info("SMS 2FA setup initiation request received");

        TwoFactorActivationResult result = smsTwoFactorService.initiateActivation();

        // Set activation token in HttpOnly cookie
        authCookieService.set2FAActivationToken(httpResponse, result.twoFaActivationToken());

        log.info("SMS 2FA setup initiated successfully");

        return ResponseEntity.ok(
                TwoFactorOperationResponse.initiateActivationSuccess(result.twoFactorType())
        );
    }

    /**
     * Verify code and activate SMS two-factor authentication.
     *
     * This endpoint completes the SMS 2FA setup process by:
     * 1. Extracting the activation token from the HttpOnly cookie
     * 2. Validating the JWT token (expiration, signature)
     * 3. Verifying the provided code against the hashed code in token
     * 4. Creating/reactivating SMS 2FA configuration
     * 5. Setting SMS 2FA as primary method
     * 6. Clearing the activation cookie
     *
     * Security measures:
     * - Rate limiting on verification attempts (5 per 15 minutes)
     * - Constant-time comparison for code verification
     * - Token expiration validation
     *
     * Response: 200 OK with standardized TwoFactorOperationResponse
     *
     * Possible errors:
     * - 400 Bad Request: Invalid or missing activation token
     * - 400 Bad Request: Invalid verification code format
     * - 401 Unauthorized: Invalid verification code
     * - 429 Too Many Requests: Rate limit exceeded
     *
     * @param activationToken JWT activation token from HttpOnly cookie
     * @param request Request body containing the 6-digit verification code
     * @param httpResponse HTTP response for clearing the activation cookie
     * @return ResponseEntity containing operation success details
     */
    @PostMapping("/setup/verify")
    public ResponseEntity<TwoFactorOperationResponse> verifySmsSetup(
            @CookieValue(name = "2fa_activation_token") String activationToken,
            @Valid @RequestBody TwoFactorVerificationRequest request,
            HttpServletResponse httpResponse
    ) {
        log.info("SMS 2FA setup verification request received");

        // Convert to BLL request with activation token
        SmsTwoFactorActivationRequest smsRequest = new SmsTwoFactorActivationRequest(
                request.verificationCode()
        );

        // Verify and activate
        smsTwoFactorService.verifyAndActivateSmsTwoFactor(smsRequest.toBll(activationToken));

        // Clear activation cookie on success
        authCookieService.clear2FAActivationToken(httpResponse);

        log.info("SMS 2FA activated successfully");

        return ResponseEntity.ok(
                TwoFactorOperationResponse.enabled(TwoFactorType.SMS)
        );
    }
}