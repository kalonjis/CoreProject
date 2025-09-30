package be.steby.CoreProject.pl.domains.auth.controller;

import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidRefreshTokenException;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import be.steby.CoreProject.pl.domains.auth.models.requests.LoginRequest;
import be.steby.CoreProject.pl.domains.auth.models.responses.AuthOperationResponse;
import be.steby.CoreProject.pl.models.user.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    private final JwtUtil jwtUtil;

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