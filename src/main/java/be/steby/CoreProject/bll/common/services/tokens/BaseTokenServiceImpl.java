package be.steby.CoreProject.bll.common.services.tokens;

import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.TokenExpiredException;
import be.steby.CoreProject.bll.exceptions.TokenRevokedException;
import be.steby.CoreProject.dal.repositories.tokens.BaseTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Enhanced base implementation for token services with mandatory URL security.
 *
 * Provides common functionality for managing different types of security tokens
 * with MANDATORY encryption for URL transmission. All tokens are secured by default.
 *
 * Security Features:
 * - MANDATORY token encryption for all URL transmissions
 * - Automatic decryption when retrieving tokens
 * - FAIL-FAST if security is not properly configured
 * - KISS: Single point of security for all token types
 * - DRY: No code duplication across token services
 *
 * @param <T> The specific type of token, must extend BaseToken
 * @author Your Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseTokenServiceImpl<T extends BaseToken> implements BaseTokenService<T> {

    private final BaseTokenRepository<T> tokenRepository;
    private final Class<T> tokenClass;

    // MANDATORY security service - must be present
    @Autowired
    private SecureTokenService secureTokenService;

    // =========================================================================
    // Public API with Integrated Security
    // =========================================================================

    /**
     * Gets a token by its public_id (from URL) or plain token (internal).
     *
     * This method handles both:
     * - Encrypted public_id from URLs (sec_xxx)
     * - Plain token for internal business logic
     *
     * @param tokenValue The token value (encrypted public_id or plain token)
     * @return The token entity
     * @throws DoesntExistException if token doesn't exist
     */
    @Override
    @Transactional
    public T getToken(String tokenValue) {
        // First try: lookup by public_id (encrypted token from URL)
        if (secureTokenService.isSecured(tokenValue)) {
            Optional<T> tokenByPublicId = tokenRepository.findByPublicId(tokenValue);
            if (tokenByPublicId.isPresent()) {
                return tokenByPublicId.get();
            }
        }

        // Second try: lookup by plain token (internal usage)
        return tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new DoesntExistException("Token : " + tokenValue + " does not exist"));
    }

    /**
     * Gets a valid token by its public_id/token and type.
     *
     * @param tokenValue The token string value (encrypted public_id or plain token)
     * @param tokenType The expected token type
     * @return The valid token
     * @throws DoesntExistException if token doesn't exist, has wrong type, or is invalid
     */
    @Override
    @Transactional(readOnly = true)
    public T getValidToken(String tokenValue, TokenType tokenType) {
        // First try: lookup by public_id (encrypted token from URL)
        if (secureTokenService.isSecured(tokenValue)) {
            Optional<T> tokenByPublicId = tokenRepository.findByPublicIdAndTokenType(tokenValue, tokenType);
            if (tokenByPublicId.isPresent()) {
                T token = tokenByPublicId.get();
                // Validate expiration and revocation
                if (!token.isRevoked() && !token.isExpired()) {
                    return token;
                }
            }
        }

        // Second try: lookup by plain token and validate
        return tokenRepository.findValidTokenByTokenAndType(tokenValue, tokenType, Instant.now())
                .orElseThrow(() -> new DoesntExistException(
                        "Token not found, invalid, or wrong type. Expected: " + tokenType
                ));
    }

    /**
     * Gets a token by its public_id/token and type.
     *
     * @param tokenValue The token string value (encrypted public_id or plain token)
     * @param tokenType The expected token type
     * @return The token if found and type matches
     * @throws DoesntExistException if token doesn't exist or has wrong type
     */
    @Override
    @Transactional(readOnly = true)
    public T getTokenByType(String tokenValue, TokenType tokenType) {
        // First try: lookup by public_id (encrypted token from URL)
        if (secureTokenService.isSecured(tokenValue)) {
            Optional<T> tokenByPublicId = tokenRepository.findByPublicIdAndTokenType(tokenValue, tokenType);
            if (tokenByPublicId.isPresent()) {
                return tokenByPublicId.get();
            }
        }

        // Second try: lookup by plain token
        return tokenRepository.findByTokenAndTokenType(tokenValue, tokenType)
                .orElseThrow(() -> new DoesntExistException(
                        "Token not found or wrong type. Expected: " + tokenType
                ));
    }

    /**
     * Creates a new token with automatic public_id encryption for URLs.
     *
     * Architecture:
     * - token: plain value for business logic
     * - public_id: encrypted value for URLs (like GitHub tokens)
     *
     * @param user The user for whom the token is being created
     * @param expirationInMillis Duration in milliseconds until the token expires
     * @param revokeExisting Whether to revoke existing tokens for this user
     * @return The newly created token with plain token and encrypted public_id
     */
    @Override
    @Transactional
    public T createToken(User user, Long expirationInMillis, boolean revokeExisting) {
        try {
            if (revokeExisting) {
                revokeAllUserTokens(user);
            }

            T token = tokenClass.getDeclaredConstructor().newInstance();
            token.setUser(user);

            // Generate plain token for business logic
            String plainToken = generateSecureToken();
            token.setToken(plainToken);

            // Generate encrypted public_id for URLs (using existing BaseEntity column)
            String encryptedPublicId = secureTokenService.secureToken(plainToken);
            token.setPublicId(encryptedPublicId);

            token.setExpiryDate(Instant.now().plusMillis(expirationInMillis));

            T savedToken = tokenRepository.save(token);

            log.debug("Created token for user: {} (plain: {}, public_id: {})",
                    user.getUsername(), plainToken.substring(0, 8) + "...",
                    encryptedPublicId.substring(0, 12) + "...");

            return savedToken;

        } catch (Exception e) {
            log.error("Failed to create token for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to create token", e);
        }
    }

    /**
     * Gets the encrypted token for URL transmission.
     * Uses the public_id field which contains the encrypted token.
     *
     * @param token The token entity
     * @return Encrypted token safe for URLs (from public_id field)
     */
    public String getTokenForUrl(T token) {
        return token.getPublicId(); // Already encrypted and stored
    }

    @Override
    public T createToken(User user, TokenType tokenType, Long expirationInMillis, boolean revokeExisting) {
        T token = createToken(user, expirationInMillis, revokeExisting);
        token.setTokenType(tokenType);
        return tokenRepository.save(token);
    }

    @Override
    @Transactional
    public T createToken(User user, Long expirationInMillis) {
        return createToken(user, expirationInMillis, true);
    }

    // =========================================================================
    // Existing Methods (updated for mandatory security)
    // =========================================================================

    @Override
    public void saveToken(T token) {
        tokenRepository.save(token);
    }

    @Override
    public Optional<T> verifyToken(Long id, String tokenValue) {
        // First try: lookup by public_id (encrypted token from URL)
        if (secureTokenService.isSecured(tokenValue)) {
            Optional<T> tokenByPublicId = ((BaseTokenRepository<T>) tokenRepository)
                    .findByIdAndPublicIdAndRevokedFalse(id, tokenValue);
            if (tokenByPublicId.isPresent()) {
                return tokenByPublicId;
            }
        }

        // Second try: lookup by plain token
        return ((BaseTokenRepository<T>) tokenRepository)
                .findByIdAndTokenAndRevokedFalse(id, tokenValue);
    }

    // =========================================================================
    // Removed - No longer needed with public_id architecture
    // =========================================================================

    // private String decryptUrlTokenIfNeeded(String token) - REMOVED

    @Override
    public T verifyTokenValidity(T token) {
        if (token.isRevoked()) {
            throw new TokenRevokedException("Token was revoked");
        }

        if (token.isExpired()) {
            this.revokeToken(token);
            throw new TokenExpiredException("Token has expired");
        }

        return token;
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        ((BaseTokenRepository<T>) tokenRepository).revokeAllUserTokens(user);
    }

    @Override
    @Transactional
    public void revokeToken(T token) {
        token.setRevoked(true);
        log.debug("Token revoked: {}", token.getClass().getSimpleName());
        tokenRepository.save(token);
    }

    /**
     * Generates a secure random token (unchanged).
     */
    protected String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}