package be.steby.CoreProject.bll.domains.auth.services.jwt;

import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidAccessTokenException;
import be.steby.CoreProject.bll.domains.auth.models.AccessTokenClaims;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.jwt.JwtCoreService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain service for authentication JWT operations.
 *
 * <p>This service handles the generation and validation of access tokens
 * used for authenticating users in the application. It encapsulates all
 * business logic related to authentication tokens within the auth domain.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Generate access tokens with user and device information</li>
 *   <li>Validate access tokens and extract claims</li>
 *   <li>Transform raw JWT claims into domain-specific models</li>
 * </ul>
 *
 * <p><b>Token Claims Structure:</b>
 * <pre>{@code
 * {
 *   "sub": "username",
 *   "username": "john.doe",
 *   "mustChangePassword": false,
 *   "roles": ["ROLE_USER"],
 *   "deviceId": 123,
 *   "deviceFingerprint": "abc123...",
 *   "iat": 1234567890,
 *   "exp": 1234571490
 * }
 * }</pre>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Device fingerprint is included to detect token theft</li>
 *   <li>Token validation is delegated to {@link JwtCoreService} for cryptographic operations</li>
 *   <li>Business validation (user state, device state) must be done by the caller</li>
 * </ul>
 *
 * @author Steby Team
 * @since 2.1.0
 * @see JwtCoreService
 */
@Service
@Slf4j
public class AuthJwtService {

    private final JwtCoreService jwtCoreService;

    @Value("${domains.auth.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${domains.auth.jwt.access-token-cookie-name}")
    private String accessTokenCookieName;

    @Value("${domains.auth.jwt.refresh-token-cookie-name}")
    private String refreshTokenCookieName;

    public AuthJwtService(JwtCoreService jwtCoreService) {
        this.jwtCoreService = jwtCoreService;
    }

    /**
     * Generates an access token for an authenticated user and device.
     *
     * <p>The generated token contains all necessary claims for:
     * <ul>
     *   <li>User identification and authorization</li>
     *   <li>Device binding for security validation</li>
     *   <li>Password change enforcement flag</li>
     * </ul>
     *
     * @param user   the authenticated user
     * @param device the device from which the user is authenticating
     * @return signed JWT access token string
     */
    public String generateAccessToken(User user, Device device) {
        Map<String, Object> claims = buildAccessTokenClaims(user, device);

        String token = jwtCoreService.sign(
                claims,
                user.getUsername(),
                accessTokenExpiration
        );

        log.debug("Access token generated for user: {}, device: {}",
                user.getUsername(), device.getId());

        return token;
    }

    /**
     * Validates an access token and returns the parsed claims.
     *
     * <p>This method performs:
     * <ul>
     *   <li>Signature verification</li>
     *   <li>Expiration validation</li>
     *   <li>Required claims presence check</li>
     * </ul>
     *
     * @param token the JWT access token to validate
     * @return validated claims from the token
     * @throws InvalidAccessTokenException if token is invalid, expired, or malformed
     */
    public Claims validateAccessToken(String token) {
        try {
            Claims claims = jwtCoreService.parse(token);
            validateRequiredClaims(claims);

            log.debug("Access token validated for user: {}", claims.getSubject());
            return claims;

        } catch (JwtException e) {
            log.warn("Access token validation failed: {}", e.getMessage());
            throw new InvalidAccessTokenException("Invalid or expired access token", e);
        }
    }

    /**
     * Extracts and transforms claims into a domain-specific model.
     *
     * <p>Provides a type-safe way to access token claims without
     * dealing with raw {@link Claims} object throughout the codebase.
     *
     * @param token the JWT access token
     * @return structured access token claims
     * @throws InvalidAccessTokenException if token is invalid
     */
    public AccessTokenClaims extractClaims(String token) {
        Claims claims = validateAccessToken(token);
        return mapToAccessTokenClaims(claims);
    }

    /**
     * Returns the configured access token cookie name.
     *
     * @return cookie name for access token
     */
    public String getAccessTokenCookieName() {
        return accessTokenCookieName;
    }

    /**
     * Returns the configured refresh token cookie name.
     *
     * @return cookie name for refresh token
     */
    public String getRefreshTokenCookieName() {
        return refreshTokenCookieName;
    }

    /**
     * Returns the access token expiration in seconds.
     *
     * <p>Useful for cookie max-age configuration.
     *
     * @return expiration in seconds
     */
    public int getAccessTokenExpirationInSeconds() {
        long seconds = accessTokenExpiration / 1000;
        if (seconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("Access token expiration too large for cookie max age");
        }
        return (int) seconds;
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Builds the claims map for an access token.
     */
    private Map<String, Object> buildAccessTokenClaims(User user, Device device) {
        Map<String, Object> claims = new HashMap<>();

        // User claims
        claims.put("username", user.getUsername());
        claims.put("mustChangePassword", user.isMustChangePassword());
        claims.put("roles", extractRoleNames(user.getAuthorities()));

        // Device claims (for security validation)
        claims.put("deviceId", device.getId());
        claims.put("deviceFingerprint", device.getFingerprint());

        return claims;
    }

    /**
     * Extracts role names from authorities collection.
     */
    private Collection<String> extractRoleNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    /**
     * Validates that all required claims are present in the token.
     */
    private void validateRequiredClaims(Claims claims) {
        if (claims.get("username", String.class) == null) {
            throw new InvalidAccessTokenException("Missing username claim in access token");
        }
        if (claims.get("deviceId", Long.class) == null) {
            throw new InvalidAccessTokenException("Missing deviceId claim in access token");
        }
        if (claims.get("deviceFingerprint", String.class) == null) {
            throw new InvalidAccessTokenException("Missing deviceFingerprint claim in access token");
        }
    }

    /**
     * Maps raw JWT claims to domain-specific AccessTokenClaims model.
     */
    @SuppressWarnings("unchecked")
    private AccessTokenClaims mapToAccessTokenClaims(Claims claims) {
        return new AccessTokenClaims(
                claims.getSubject(),
                claims.get("username", String.class),
                claims.get("mustChangePassword", Boolean.class),
                (Collection<String>) claims.get("roles"),
                claims.get("deviceId", Long.class),
                claims.get("deviceFingerprint", String.class),
                claims.getIssuedAt().getTime(),
                claims.getExpiration().getTime()
        );
    }
}