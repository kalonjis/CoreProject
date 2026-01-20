package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidRefreshTokenException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidTwoFactorTokenException;
import be.steby.CoreProject.bll.domains.auth.models.LoginInitiationResult;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorMethodChosenResult;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorSessionInfo;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.pl.domains.auth.models.requests.ChooseTwoFactorMethodRequest;
import be.steby.CoreProject.pl.domains.auth.models.requests.LoginRequest;
import be.steby.CoreProject.pl.domains.auth.models.requests.TwoFactorVerificationRequest;
import be.steby.CoreProject.pl.domains.auth.models.responses.*;
import be.steby.CoreProject.pl.domains.profile.user.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller responsible for handling authentication-related HTTP requests.
 * This controller manages user authentication, token management, and session handling.
 * All business logic is delegated to appropriate services while this controller
 * focuses solely on HTTP concerns (headers, response formatting).
 * Cookie management is delegated to AuthCookieService.
 * Token generation is delegated to AuthService.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    // Service dependencies
    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final RefreshTokenServiceImpl refreshTokenService;



    // =========================================================================
    // Public Authentication Endpoints
    // =========================================================================

    /**
     * Handles user login requests.
     * Authenticates user, detects device, generates tokens, and sets secure cookies.
     * All business logic is delegated to AuthService.
     *
     * @param loginRequest Login request containing username and password
     * @param httpRequest HTTP request for device detection
     * @param httpResponse HTTP response for cookie setting
     * @return ResponseEntity with login status
     */
    @PreAuthorize("isAnonymous()")
    @PostMapping("/login")
    public ResponseEntity<AuthOperationResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Login attempt for username: {}", loginRequest.username());

        // BLL: Complete login (auth + device detection + token generation)
        LoginTokens tokens = authService.login(
                loginRequest.username(),
                loginRequest.password(),
                httpRequest
        );

        // PL: HTTP handling (cookies only)
        authCookieService.setAuthenticationCookies(httpResponse, tokens);

        // Minimal informational headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Status", "authenticated");

        log.info("Login successful");
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(AuthOperationResponse.loginSuccessful());
    }


    /**
     * Phase 1: Initial login attempt - validates credentials and checks 2FA requirements
     *
     * @param loginRequest Login request containing username and password
     * @param httpRequest HTTP request for device detection
     * @param httpResponse HTTP response for cookie setting
     * @return ResponseEntity with login status (complete or requires 2FA)
     */
    @PreAuthorize("isAnonymous()")
    @PostMapping("/initiate-login")
    public ResponseEntity<AuthOperationResponse> initiateLogin(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Login initiation attempt for username: {}", loginRequest.username());

        // BLL: Authenticate user and check 2FA requirements
        LoginInitiationResult result = authService.initiateLogin(
                loginRequest.username(),
                loginRequest.password(),
                httpRequest
        );

        if (result.requiresTwoFactor()) {
            // Set 2FA token cookie for verification phase
            authCookieService.set2FASessionToken(httpResponse, result.sessionToken());

            log.info("2FA required for user: {}", loginRequest.username());
            return ResponseEntity.ok(AuthOperationResponse.twoFactorRequired());
        } else {
            // Complete login immediately - no 2FA required
            authCookieService.setAuthenticationCookies(httpResponse, result.loginTokens());

            log.info("Login completed without 2FA for user: {}", loginRequest.username());
            return ResponseEntity.ok(AuthOperationResponse.loginSuccessful());
        }
    }


    /**
     * Get available two-factor authentication methods for the authenticated user.
     *
     * This endpoint returns the list of 2FA methods that are enabled for the
     * authenticated user. Used in security settings to show configured 2FA methods.
     *
     * @param user the authenticated user from security context
     * @return ResponseEntity with list of available 2FA methods
     */
    @GetMapping("/2fa/available-methods")
    public ResponseEntity<List<TwoFactorAuthDTO>> getAvailableTwoFactorMethods(
            @AuthenticationPrincipal User user) {

        log.debug("Getting available 2FA methods for authenticated user: {}", user.getUsername());

        // Get entities from service
        List<TwoFactorAuth> enabledMethods = authService.getAvailableTwoFactorMethods(user);

        // Transform to DTOs in presentation layer
        List<TwoFactorAuthDTO> response = enabledMethods.stream()
                .map(TwoFactorAuthDTO::fromEntity)
                .toList();

        log.debug("Found {} available 2FA methods", response.size());

        return ResponseEntity.ok(response);
    }


    /**
     * Get all two-factor authentication methods with their status.
     *
     * Returns ALL supported 2FA methods (EMAIL, SMS, TOTP, BACKUP_CODES),
     * each with its enabled/disabled status for the authenticated user.
     * Used in security settings to display all available methods.
     *
     * Unlike /2fa/available-methods which returns only enabled methods,
     * this endpoint returns all supported methods for the settings UI.
     *
     * @param user the authenticated user from security context
     * @return ResponseEntity with list of all 2FA methods and their status
     */
    @GetMapping("/2fa/settings")
    public ResponseEntity<List<TwoFactorAuthDTO>> getAllTwoFactorMethods(
            @AuthenticationPrincipal User user) {

        log.debug("Getting all 2FA methods with status for user: {}", user.getUsername());

        // Get all methods via AuthService (not directly from factory)
        List<TwoFactorAuth> allMethods = authService.getAllTwoFactorMethodsWithStatus(user);

        // Transform to DTOs
        List<TwoFactorAuthDTO> response = allMethods.stream()
                .map(auth -> {
                    if (auth.getId() != null) {
                        return TwoFactorAuthDTO.fromEntity(auth);
                    } else {
                        return TwoFactorAuthDTO.notConfigured(user.getPublicId(), auth.getType());
                    }
                })
                .toList();

        log.debug("Returning {} 2FA methods", response.size());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/2fa/login/methods")
    public ResponseEntity<List<TwoFactorAuthDTO>> getEnabledTwoFactorMethods(
            @CookieValue(name = "2fa_session_token") String twoFactorSessionToken) {

        // Get entities from service
        List<TwoFactorAuth> enabledMethods = authService.getEnabledTwoFactorMethods(twoFactorSessionToken);

        // Transform to DTOs in presentation layer
        List<TwoFactorAuthDTO> response = enabledMethods.stream()
                .map(TwoFactorAuthDTO::fromEntity)
                .toList();

        log.debug("Found {} available 2FA methods", response.size());

        return ResponseEntity.ok(response);
    }


    /**
     * Phase 2: Choose 2FA method
     *
     * User selects which 2FA method to use from their available options during login flow.
     * For methods requiring codes (EMAIL, SMS), a verification code is generated and sent.
     * For TOTP and backup codes, no code generation is needed.
     *
     * Returns 503 Service Unavailable if code delivery fails (EMAIL/SMS unavailable).
     * In this case, alternativeMethods contains fallback options (TOTP, BACKUP_CODES).
     *
     * @param request the chosen 2FA method
     * @param twoFactorSessionToken JWT session token from cookie
     * @param httpRequest HTTP request for context
     * @param httpResponse HTTP response to manage cookies
     * @return ResponseEntity with method selection result
     */
    @PreAuthorize("isAnonymous()")
    @PostMapping("/2fa/choose-method")
    public ResponseEntity<TwoFactorMethodChosenResponse> chooseTwoFactorMethod(
            @Valid @RequestBody ChooseTwoFactorMethodRequest request,
            @CookieValue(name = "2fa_session_token") String twoFactorSessionToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Choose 2FA method request: {}", request.twoFactorType());

        // BLL: Process method selection and get result with token
        TwoFactorMethodChosenResult result = authService.chooseTwoFactorMethod(
                twoFactorSessionToken,
                request.twoFactorType(),
                httpRequest
        );

        // Handle delivery failure - return 503 with alternatives
        if (result.deliveryFailed()) {
            log.warn("2FA code delivery failed for method: {} - Alternatives: {}",
                    result.chosenMethod(), result.alternativeMethods());

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(TwoFactorMethodChosenResponse.deliveryFailed(result));
        }

        // Success - set token cookie and return success response
        authCookieService.set2FAToken(httpResponse, result.twoFactorToken());

        log.info("2FA method chosen successfully: {} - Code generated: {}",
                request.twoFactorType(), result.codeGenerated());

        return ResponseEntity.ok(TwoFactorMethodChosenResponse.success(result));
    }

    /**
     * Phase 3: Two-factor authentication verification
     * Validates the verification code and completes login if successful
     *
     * @param request Verification request containing the 6-digit code
     * @param twoFactorTokenCookie JWT token from 2FA cookie
     * @param httpRequest HTTP request for context
     * @param httpResponse HTTP response for setting final auth cookies
     * @return ResponseEntity with verification result
     */
    @PreAuthorize("isAnonymous()")
    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthOperationResponse> verifyTwoFactor(
            @Valid @RequestBody TwoFactorVerificationRequest request,
            @CookieValue(name = "2fa_token", required = false) String twoFactorTokenCookie,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (twoFactorTokenCookie == null) {
            throw new InvalidTwoFactorTokenException("2FA token is required");
        }

        log.info("2FA verification attempt with code type: {}",
                request.hasVerificationCode() ? "numeric" : "backup");

        // BLL: Verify 2FA code and complete login
        LoginTokens tokens = authService.verifyTwoFactorAndCompleteLogin(
                twoFactorTokenCookie,
                request.verificationCode(),  // Peut être null
                request.backupCode(),        // Peut être null
                httpRequest
        );

        // PL: Clear 2FA token and set final auth cookies
        authCookieService.clear2FAToken(httpResponse);
        authCookieService.setAuthenticationCookies(httpResponse, tokens);

        log.info("2FA verification successful - login completed");

        return ResponseEntity.ok(AuthOperationResponse.loginSuccessful());
    }

    /**
     * Resend 2FA verification code
     *
     * Allows user to request a new verification code if the original was not received.
     * Only applicable for EMAIL and SMS methods.
     *
     * Returns 503 Service Unavailable if code delivery fails.
     * The TwoFactorCodeDeliveryException is handled by GlobalExceptionHandler.
     *
     * @param twoFactorTokenCookie JWT token from 2FA cookie
     * @param httpRequest HTTP request for context
     * @return ResponseEntity with resend confirmation
     */
    @PreAuthorize("isAnonymous()")
    @PostMapping("/resend-2fa-code")
    public ResponseEntity<AuthOperationResponse> resendTwoFactorCode(
            @CookieValue(name = "2fa_token", required = false) String twoFactorTokenCookie,
            HttpServletRequest httpRequest) {

        if (twoFactorTokenCookie == null) {
            throw new InvalidTwoFactorTokenException("2FA token is required");
        }

        log.info("2FA code resend requested");

        // BLL: Validate 2FA token and resend verification code
        // Throws TwoFactorCodeDeliveryException if delivery fails
        authService.resendTwoFactorCode(twoFactorTokenCookie, httpRequest);

        log.info("2FA code resent successfully");
        return ResponseEntity.ok(AuthOperationResponse.twoFactorCodeResent());
    }

    /**
     * Get 2FA status during authentication flow
     * Allows frontend to check current 2FA state
     *
     * @param twoFactorTokenCookie JWT token from 2FA cookie
     * @return ResponseEntity with 2FA status information
     */
    @PreAuthorize("isAnonymous()")
    @GetMapping("/2fa-status")
    public ResponseEntity<Map<String, Object>> getTwoFactorStatus(
            @CookieValue(name = "2fa_token", required = false) String twoFactorTokenCookie) {

        Map<String, Object> response = new HashMap<>();

        if (twoFactorTokenCookie == null) {
            response.put("twoFactorRequired", false);
            response.put("status", "no_2fa_session");
            return ResponseEntity.ok(response);
        }

        try {
            // BLL: Get 2FA session info from token
            TwoFactorSessionInfo sessionInfo = authService.getTwoFactorSessionInfo(twoFactorTokenCookie);

            response.put("twoFactorRequired", true);
            response.put("status", "awaiting_verification");
            response.put("type", sessionInfo.twoFactorType().name());
            response.put("maskedEmail", sessionInfo.maskedEmail());
            response.put("timeRemaining", sessionInfo.timeRemainingSeconds());

            return ResponseEntity.ok(response);

        } catch (InvalidTwoFactorTokenException e) {
            response.put("twoFactorRequired", false);
            response.put("status", "invalid_2fa_session");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Provides current authentication status.
     * Used by frontend to check if user is authenticated.
     *
     * @param user Currently authenticated user (null if not authenticated)
     * @return ResponseEntity with authentication status and user details
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthenticationStatus(
            @AuthenticationPrincipal User user) {

        Map<String, Object> response = buildAuthStatusResponse(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns current authenticated user details.
     *
     * @param user Currently authenticated user
     * @return ResponseEntity with user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }


    /**
     * Returns current authenticated user details for session front side.
     *
     * @param user Currently authenticated user
     * @return ResponseEntity with user information
     */
    @GetMapping("/session")
    public ResponseEntity<UserSessionResponse> getUserSessionInfo(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserSessionResponse.fromEntity(user));
    }



    /**
     * Refreshes access token using refresh token cookie.
     * Validates refresh token and generates new access token.
     *
     * @param refreshTokenCookie Refresh token from HTTP cookie
     * @param response HTTP response for new cookie setting
     * @return ResponseEntity indicating refresh status
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthOperationResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletResponse response) {

        if (refreshTokenCookie == null) {
            throw new InvalidRefreshTokenException("Refresh token is required");
        }

        log.debug("Processing refresh token request");

        // Parse cookie and validate token via BLL
        String[] parts = authCookieService.parseRefreshTokenCookie(refreshTokenCookie);
        long userId = Long.parseLong(parts[0]);
        long deviceId = Long.parseLong(parts[1]);
        String tokenValue = parts[2];

        RefreshToken validatedToken = refreshTokenService.validateRefreshToken(tokenValue, userId, deviceId);

        // BLL: Generate new tokens
        LoginTokens tokens = authService.refreshAuthTokens(validatedToken);

        // PL: Set new cookies
        authCookieService.setAuthenticationCookies(
                response,
                tokens);

        log.debug("Token refresh successful for user: {}", validatedToken.getUser().getUsername());
        return ResponseEntity.ok(AuthOperationResponse.tokenRefreshed());
    }

    /**
     * Handles user logout requests.
     * Delegates business logic to service and clears HTTP cookies.
     *
     * @param refreshTokenCookie Refresh token from HTTP cookie
     * @param request HTTP request for context capture
     * @param response HTTP response for cookie clearing
     * @return ResponseEntity with logout confirmation
     */
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<AuthOperationResponse> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // Delegate business logic to service (token revocation, events publishing)
            authService.logout(refreshTokenCookie, request);
            log.info("Logout request processed successfully");
        } catch (Exception e) {
            log.warn("Error during logout process: {}", e.getMessage());
        } finally {
            // Always clear cookies regardless of business logic outcome
            authCookieService.clearAuthenticationCookies(response);
        }

        return ResponseEntity.ok(AuthOperationResponse.logoutSuccessful());
    }

    // =========================================================================
    // Private Helper Methods - HTTP Layer Only
    // =========================================================================

    /**
     * Builds authentication status response for HTTP.
     * This is presentation logic (formatting data for API response).
     */
    private Map<String, Object> buildAuthStatusResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        boolean isAuthenticated = user != null;

        response.put("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            response.put("username", user.getUsername());
            response.put("roles", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        }

        return response;
    }
}