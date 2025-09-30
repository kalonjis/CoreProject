package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidRefreshTokenException;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.userRegistration.services.UserRegistrationService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import be.steby.CoreProject.pl.domains.auth.models.requests.LoginRequest;
import be.steby.CoreProject.pl.domains.auth.models.responses.AuthOperationResponse;
import be.steby.CoreProject.pl.models.user.UserDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller responsible for handling authentication-related HTTP requests.
 * This controller manages user authentication, token management, and session handling.
 * All business logic is delegated to appropriate services while this controller
 * focuses solely on HTTP concerns (cookies, headers, response formatting).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    // Service dependencies
    private final AuthService authService;
    private final UserRegistrationService userRegistrationService;
    private final RefreshTokenServiceImpl refreshTokenService;

    // JWT utilities
    private final JwtUtil jwtUtil;

    // Configuration properties
    @Value("${security.jwt.access-token.name}")
    private String accessTokenCookieName;

    @Value("${security.jwt.refresh-token.name}")
    private String refreshTokenCookieName;

    @Value("${security.jwt.access-token.expiration}")
    private Long accessTokenDurationMs;

    @Value("${security.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;

    private static final String COOKIE_PATH = "/";

    // =========================================================================
    // Public Authentication Endpoints
    // =========================================================================


    /**
     * Handles user login requests.
     * Authenticates user, detects device, generates tokens, and sets secure cookies.
     *
     * @param loginRequest Login request containing username and password
     * @param httpRequest HTTP request for device detection
     * @param httpResponse HTTP response for cookie setting
     * @return ResponseEntity with login status and user information
     */
    @PreAuthorize("isAnonymous()")
    @PostMapping("/login")
    public ResponseEntity<AuthOperationResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse) {

        log.info("Login attempt for username: {}", loginRequest.username());

        // BLL : authentification
        User user = authService.login(loginRequest.username(), loginRequest.password(), httpRequest);
        Device device = extractDeviceFromRequest(httpRequest);

        // PL : gestion HTTP (cookies + headers)
        handleSuccessfulLogin(user, device, httpResponse);

        // Headers informatifs minimaux
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Status", "authenticated");

        log.info("Login successful for user: {}", user.getUsername());
        return ResponseEntity
            .ok()
            .headers(headers)
            .body(AuthOperationResponse.loginSuccessful());
    }

    /**
     * Provides current authentication status.
     * Used by frontend to check if user is authenticated.
     *
     * @param user Currently authenticated user (null if not authenticated)
     * @return ResponseEntity with authentication status and user details
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthenticationStatus(@AuthenticationPrincipal User user) {
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

        // Parse and validate refresh token (lance InvalidRefreshTokenException si problème)
        RefreshToken validatedToken = validateAndParseRefreshToken(refreshTokenCookie);

        // Generate new tokens
        RefreshToken newRefreshToken = refreshTokenService.rotateToken(validatedToken);
        String newAccessToken = jwtUtil.generateAccessToken(
                newRefreshToken.getUser(),
                newRefreshToken.getDevice()
        );

        // Set new cookies
        setTokenCookies(response, newAccessToken, formatRefreshTokenCookie(newRefreshToken));

        log.debug("Token refresh successful for user: {}", newRefreshToken.getUser().getUsername());
        return ResponseEntity.ok(AuthOperationResponse.tokenRefreshed());
    }

    /**
     * Handles user logout requests.
     * Delegates business logic to service and handles HTTP cookie clearing.
     *
     * @param refreshTokenCookie Refresh token from HTTP cookie
     * @param request HTTP request for context capture
     * @param response HTTP response for cookie clearing
     * @return ResponseEntity with no content status
     */
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<AuthOperationResponse> logout(@CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
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
            clearTokenCookies(response);
        }

        return ResponseEntity.ok(AuthOperationResponse.logoutSuccessful());
    }

    // =========================================================================
    // Private Helper Methods - Login Process
    // =========================================================================

    /**
     * Extracts device information from request attributes.
     * Device is set by AuthService during login process.
     */
    private Device extractDeviceFromRequest(HttpServletRequest request) {
        Device device = (Device) request.getAttribute("currentDevice");
        if (device == null) {
            throw new IllegalStateException("Device information not found in request");
        }
        return device;
    }

    /**
     * Handles successful login by generating tokens and setting cookies.
     */
    private void handleSuccessfulLogin(User user, Device device, HttpServletResponse response) {
        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user, device);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, device);

        // Set secure cookies
        setTokenCookies(response, accessToken, formatRefreshTokenCookie(refreshToken));
    }

    /**
     * Builds success response for login endpoint.
     */
    private Map<String, Object> buildLoginSuccessResponse(User user, Device device) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("user", UserDTO.fromEntity(user));
        response.put("deviceId", device.getId());
        response.put("deviceConfirmed", device.isConfirmed());
        return response;
    }

    /**
     * Handles authentication failures (401 Unauthorized).
     * Used for invalid credentials and account activation issues.
     */
    private ResponseEntity<?> handleAuthenticationFailure(String error, String message) {
        Map<String, Object> errorResponse = createErrorResponse(error, message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles security-related failures (403 Forbidden).
     * Used for blacklisted devices and disabled accounts.
     */
    private ResponseEntity<?> handleSecurityFailure(String error, String message) {
        Map<String, Object> errorResponse = createErrorResponse(error, message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles unexpected failures (500 Internal Server Error).
     * Used for system errors and unexpected exceptions.
     */
    private ResponseEntity<?> handleGenericFailure(String error, String message) {
        Map<String, Object> errorResponse = createErrorResponse(error, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // =========================================================================
    // Private Helper Methods - Authentication Status
    // =========================================================================

    /**
     * Builds authentication status response.
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

    // =========================================================================
    // Private Helper Methods - Token Management
    // =========================================================================

    /**
     * Validates and parses refresh token from cookie format.
     * @throws InvalidRefreshTokenException if token is invalid
     */
    private RefreshToken validateAndParseRefreshToken(String refreshTokenCookie) {
        String[] parts = refreshTokenCookie.split("\\.", 2);
        if (parts.length != 2) {
            throw new InvalidRefreshTokenException("Invalid refresh token format");
        }

        try {
            Long tokenId = Long.parseLong(parts[0]);
            String tokenValue = parts[1];

            return refreshTokenService.verifyToken(tokenId, tokenValue)
                    .orElseThrow(() -> new InvalidRefreshTokenException("Token not found or invalid"));

        } catch (NumberFormatException e) {
            throw new InvalidRefreshTokenException("Invalid token ID format");
        }
    }

    /**
     * Formats refresh token for cookie storage.
     */
    private String formatRefreshTokenCookie(RefreshToken refreshToken) {
        return refreshToken.getId() + "." + refreshToken.getToken();
    }

    /**
     * Revokes refresh token during logout.
     */
    private void revokeRefreshToken(String refreshTokenCookie) {
        try {
            String[] parts = refreshTokenCookie.split("\\.", 2);
            if (parts.length == 2) {
                Long tokenId = Long.parseLong(parts[0]);
                String tokenValue = parts[1];

                refreshTokenService.verifyToken(tokenId, tokenValue)
                        .ifPresent(token -> {
                            token.setRevoked(true);
                            refreshTokenService.saveToken(token);
                            log.debug("Refresh token {} revoked successfully", tokenId);
                        });
            }
        } catch (Exception e) {
            log.warn("Error revoking refresh token: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Private Helper Methods - Cookie Management
    // =========================================================================

    /**
     * Sets both access and refresh token cookies.
     */
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshTokenCookie) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshTokenCookie);
    }

    /**
     * Sets access token cookie with appropriate security settings.
     */
    private void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(accessTokenCookieName, token);
        configureCookie(cookie, (int) (accessTokenDurationMs / 1000));
        response.addCookie(cookie);
    }

    /**
     * Sets refresh token cookie with appropriate security settings.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String tokenValue) {
        Cookie cookie = new Cookie(refreshTokenCookieName, tokenValue);
        configureCookie(cookie, (int) (refreshTokenDurationMs / 1000));
        response.addCookie(cookie);
    }

    /**
     * Clears both access and refresh token cookies.
     */
    private void clearTokenCookies(HttpServletResponse response) {
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    /**
     * Clears access token cookie by setting it to expire immediately.
     */
    private void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(accessTokenCookieName, "");
        configureCookieForDeletion(cookie);
        response.addCookie(cookie);
    }

    /**
     * Clears refresh token cookie by setting it to expire immediately.
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenCookieName, "");
        configureCookieForDeletion(cookie);
        response.addCookie(cookie);
    }

    /**
     * Configures cookie with standard security settings.
     */
    private void configureCookie(Cookie cookie, int maxAge) {
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAge);
    }

    /**
     * Configures cookie for deletion (maxAge = 0).
     */
    private void configureCookieForDeletion(Cookie cookie) {
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0);
    }

    // =========================================================================
    // Private Helper Methods - Response Building
    // =========================================================================

    /**
     * Creates standard error response structure.
     */
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        return response;
    }

    /**
     * Creates detailed error response structure.
     */
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}