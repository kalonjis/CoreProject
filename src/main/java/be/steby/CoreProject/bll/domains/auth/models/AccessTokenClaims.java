package be.steby.CoreProject.bll.domains.auth.models;

import java.util.Collection;

/**
 * Domain model representing validated access token claims.
 *
 * <p>This record provides a type-safe, immutable representation of the claims
 * extracted from a validated JWT access token. It eliminates the need to work
 * with raw {@link io.jsonwebtoken.Claims} throughout the codebase.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * AccessTokenClaims claims = authJwtService.extractClaims(token);
 * String username = claims.username();
 * Long deviceId = claims.deviceId();
 *
 * if (claims.mustChangePassword()) {
 *     // Redirect to password change page
 * }
 * }</pre>
 *
 * @param subject            the JWT subject (typically username)
 * @param username           the user's username
 * @param mustChangePassword flag indicating if user must change password
 * @param roles              collection of role names (e.g., "ROLE_USER", "ROLE_ADMIN")
 * @param deviceId           the ID of the device that generated this token
 * @param deviceFingerprint  the fingerprint of the device for validation
 * @param issuedAt           token creation timestamp in milliseconds
 * @param expiresAt          token expiration timestamp in milliseconds
 *
 * @author Steby Team
 * @since 2.1.0
 */
public record AccessTokenClaims(
        String subject,
        String username,
        Boolean mustChangePassword,
        Collection<String> roles,
        Long deviceId,
        String deviceFingerprint,
        Long issuedAt,
        Long expiresAt
) {

    /**
     * Checks if the token has expired.
     *
     * @return true if the token has expired, false otherwise
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Checks if the user has a specific role.
     *
     * @param role the role to check (e.g., "ROLE_ADMIN")
     * @return true if the user has the specified role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Checks if the user is an administrator.
     *
     * @return true if the user has the ROLE_ADMIN role
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Returns the remaining validity time in milliseconds.
     *
     * @return remaining time until expiration, or 0 if already expired
     */
    public long remainingValidityMs() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}