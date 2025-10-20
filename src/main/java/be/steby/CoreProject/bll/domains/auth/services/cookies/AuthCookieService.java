package be.steby.CoreProject.bll.domains.auth.services.cookies;

import be.steby.CoreProject.bll.common.services.cookies.BaseCookieService;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidRefreshTokenException;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Business service for managing authentication-related cookies.
 * Handles access token and refresh token cookie creation, formatting, and deletion.
 * Delegates low-level cookie operations to BaseCookieService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthCookieService {

    private final BaseCookieService baseCookieService;

    // Configuration from application properties
    @Value("${security.jwt.access-token.name}")
    private String accessTokenCookieName;

    @Value("${security.jwt.refresh-token.name}")
    private String refreshTokenCookieName;

    @Value("${security.jwt.access-token.expiration}")
    private Long accessTokenDurationMs;

    @Value("${security.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;

    @Value("${security.jwt.2fa-token.name}")
    private String twoFactorTokenCookieName;

    @Value("${security.jwt.2fa-token.expiration}")
    private Long twoFactorTokenDurationMs;

    private static final String COOKIE_PATH = "/";
    private static final boolean SECURE = true; // Always use HTTPS in production

    /**
     * Sets both access and refresh token cookies in a single operation.
     * Used during login and token refresh flows.
     *
     * @param response HTTP response
     * @param tokens LoginTokens containing all necessary token data
     */
    public void setAuthenticationCookies(HttpServletResponse response, LoginTokens tokens) {
        setAccessTokenCookie(response, tokens.accessToken());
        setRefreshTokenCookie(response, tokens.getFormattedRefreshToken());
        log.info("Authentication cookies set successfully");
    }

    /**
     * Sets the access token cookie.
     * HttpOnly and Secure for protection against XSS and man-in-the-middle attacks.
     *
     * @param response HTTP response
     * @param accessToken JWT access token value
     */
    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        int maxAgeSeconds = (int) (accessTokenDurationMs / 1000);

        baseCookieService.setHttpOnlyCookie(
                response,
                accessTokenCookieName,
                accessToken,
                maxAgeSeconds,
                COOKIE_PATH,
                SECURE
        );

        log.debug("Access token cookie set with expiration: {} seconds", maxAgeSeconds);
    }

    /**
     * Sets the refresh token cookie.
     * HttpOnly and Secure for maximum security.
     *
     * @param response HTTP response
     * @param formattedRefreshToken Formatted refresh token (userId.deviceId.tokenValue)
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String formattedRefreshToken) {
        int maxAgeSeconds = (int) (refreshTokenDurationMs / 1000);

        baseCookieService.setHttpOnlyCookie(
                response,
                refreshTokenCookieName,
                formattedRefreshToken,
                maxAgeSeconds,
                COOKIE_PATH,
                SECURE
        );

        log.debug("Refresh token cookie set with expiration: {} seconds", maxAgeSeconds);
    }

    /**
     * Clears both authentication cookies.
     * Used during logout or when authentication needs to be reset.
     *
     * @param response HTTP response
     */
    public void clearAuthenticationCookies(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, accessTokenCookieName);
        baseCookieService.deleteHttpOnlyCookie(response, refreshTokenCookieName);
        baseCookieService.deleteHttpOnlyCookie(response, twoFactorTokenCookieName);
        log.info("Authentication cookies cleared");
    }

    /**
     * Clears only the access token cookie.
     * Useful for invalidating access without affecting refresh token.
     *
     * @param response HTTP response
     */
    public void clearAccessTokenCookie(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, accessTokenCookieName);
        log.debug("Access token cookie cleared");
    }

    /**
     * Clears only the refresh token cookie.
     * Useful for invalidating refresh without affecting active access token.
     *
     * @param response HTTP response
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, refreshTokenCookieName);
        log.debug("Refresh token cookie cleared");
    }

    /**
     * Parses a refresh token cookie value back into its components.
     * Expected format: userId.deviceId.tokenValue
     *
     * @param cookieValue Raw cookie value
     * @return Array containing [userId, deviceId, tokenValue]
     * @throws InvalidRefreshTokenException if format is invalid
     */
    public String[] parseRefreshTokenCookie(String cookieValue) {
        if (cookieValue == null || cookieValue.isEmpty()) {
            throw new InvalidRefreshTokenException("Refresh token cookie value cannot be null or empty");
        }

        String[] parts = cookieValue.split("\\.", 3);

        if (parts.length != 3) {
            log.warn("Invalid refresh token cookie format received");
            throw new InvalidRefreshTokenException("Invalid refresh token format");
        }

        log.debug("Refresh token cookie parsed successfully");
        return parts; // [userId, deviceId, tokenValue]
    }


    /**
     * Sets the 2FA token cookie during authentication flow.
     * HttpOnly and Secure for protection, shorter expiration time.
     *
     * @param response HTTP response
     * @param twoFactorToken JWT 2FA token value
     */
    public void set2FAToken(HttpServletResponse response, String twoFactorToken) {
        int maxAgeSeconds = (int) (twoFactorTokenDurationMs / 1000);

        baseCookieService.setHttpOnlyCookie(
                response,
                twoFactorTokenCookieName,
                twoFactorToken,
                maxAgeSeconds,
                COOKIE_PATH,
                SECURE
        );

        log.debug("2FA token cookie set with expiration: {} seconds", maxAgeSeconds);
    }

    /**
     * Clears the 2FA token cookie.
     * Used after successful 2FA verification or on timeout/failure.
     *
     * @param response HTTP response
     */
    public void clear2FAToken(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, twoFactorTokenCookieName);
        log.debug("2FA token cookie cleared");
    }


    /**
     * Set 2FA session token cookie for method selection phase.
     *
     * This lightweight token is used before the user chooses a specific 2FA method.
     * Contains only user identification without verification code data.
     *
     * @param response HTTP response to set the cookie on
     * @param sessionToken the lightweight JWT token for method selection
     */
    public void set2FASessionToken(HttpServletResponse response, String sessionToken) {
        int maxAgeSeconds = (int) (twoFactorTokenDurationMs / 1000);

        baseCookieService.setHttpOnlyCookie(
                response,
                "2fa_session_token",
                sessionToken,
                maxAgeSeconds,
                COOKIE_PATH,
                SECURE
        );

        log.debug("2FA session token cookie set with expiration: {} seconds", maxAgeSeconds);
    }

    /**
     * Clear 2FA session token cookie.
     *
     * Used to remove the lightweight session token when:
     * - User chooses a 2FA method (replaced by full 2FA token)
     * - Session expires or is cancelled
     * - Authentication flow is completed or aborted
     *
     * @param response HTTP response to clear the cookie on
     */
    public void clear2FASessionToken(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, "2fa_session_token");
        log.debug("2FA session token cookie cleared");
    }




}