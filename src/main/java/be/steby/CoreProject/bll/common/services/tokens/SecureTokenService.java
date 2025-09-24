package be.steby.CoreProject.bll.common.services.tokens;

/**
 * Common service interface for securing tokens transmitted via URLs.
 *
 * This service is used across all domains (account, device, password, etc.)
 * to provide consistent token security for email links.
 *
 * Security Requirements:
 * - All tokens sent via email URLs must be secured
 * - AES-256-GCM encryption with authentication
 * - URL-safe encoding for email compatibility
 * - Fail-fast behavior for security issues
 *
 * @author Your Team
 * @since 1.0.0
 */
public interface SecureTokenService {

    /**
     * Secures a token for safe transmission via URLs.
     *
     * @param plainToken The original token to secure
     * @return Secured token safe for URL transmission
     * @throws IllegalArgumentException if plainToken is null or empty
     * @throws SecurityException if securing fails
     */
    String secureToken(String plainToken);

    /**
     * Recovers the original token from a secured token.
     *
     * @param securedToken The secured token from URL
     * @return Original plain token
     * @throws IllegalArgumentException if securedToken is null or empty
     * @throws SecurityException if recovery fails or token is invalid
     */
    String recoverToken(String securedToken);

    /**
     * Determines if a token is in secured format.
     *
     * @param token Token to check
     * @return true if token is secured, false otherwise
     */
    boolean isSecured(String token);
}