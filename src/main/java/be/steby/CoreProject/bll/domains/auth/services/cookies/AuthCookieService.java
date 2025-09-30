package be.steby.CoreProject.bll.domains.auth.services;

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
}