package be.steby.CoreProject.bll.domains.auth.models;

import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidAccessTokenException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidRefreshTokenException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidTokenException;

/**
 * Data Transfer Object for authentication tokens.
 * Contains only primitive/String data - NO entities.
 *
 * ⚠️ SECURITY: This DTO is safe to pass between layers as it contains
 * no sensitive entity references, only the token values needed for cookies.
 *
 * Used in:
 * - Login flow: AuthService → AuthController → AuthCookieService
 * - Refresh flow: AuthService → AuthController → AuthCookieService
 */
public record LoginTokens(
        String accessToken,
        String refreshTokenValue,
        long userId,
        long deviceId
) {
    /**
     * Validates that all required fields are present.
     */
    public LoginTokens {
        if (accessToken == null || accessToken.isBlank()) {
            throw new InvalidAccessTokenException("Access token cannot be null or blank");
        }
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token value cannot be null or blank");
        }
        if (userId <= 0) {
            throw new InvalidTokenException("User ID must be positive");
        }
        if (deviceId <= 0) {
            throw new InvalidTokenException ("Device ID must be positive");
        }
    }

    /**
     * Formats the refresh token for cookie storage.
     * Format: userId.deviceId.tokenValue
     *
     * @return Formatted refresh token string for cookie
     */
    public String getFormattedRefreshToken() {
        return String.format("%d.%d.%s", userId, deviceId, refreshTokenValue);
    }
}