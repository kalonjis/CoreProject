package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.exceptions.TokenRefreshExpiredException;
import be.steby.CoreProject.bll.exceptions.TokenRefreshRevokedException;
import be.steby.CoreProject.bll.services.security.BaseTokenService;
import be.steby.CoreProject.dal.repositories.tokens.BaseTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Base implementation for token services. Provides common functionality for
 * managing different types of security tokens (refresh, password reset, etc.).
 * Implements secure token generation and management with proper expiration and revocation.
 *
 * @param <T> The specific type of token, must extend BaseToken
 */
@RequiredArgsConstructor
public abstract class BaseTokenServiceImpl<T extends BaseToken> implements BaseTokenService<T> {

    private final BaseTokenRepository<T> tokenRepository;
    private final Class<T> tokenClass;

    /**
     * Creates a new token for a user with specified expiration time.
     *
     * @param user The user for whom the token is being created
     * @param expirationInMillis Duration in milliseconds until the token expires
     * @return The newly created token
     * @throws RuntimeException if token creation fails
     */
    @Override
    @Transactional
    public T createToken(User user, Long expirationInMillis) {
        try {
            revokeAllUserTokens(user);

            T token = tokenClass.getDeclaredConstructor().newInstance();
            token.setUser(user);
            token.setToken(generateSecureToken());
            token.setExpiryDate(Instant.now().plusMillis(expirationInMillis));

            return tokenRepository.save(token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create token", e);
        }
    }



    @Override
    public void saveToken(T token) {
        tokenRepository.save(token);
    }

    @Override
    @Transactional
    public T rotateToken(T oldToken) {
        // Révoquer l'ancien token
        revokeToken(oldToken);

        // Créer un nouveau token pour l'utilisateur
        return createToken(oldToken.getUser(),
                oldToken.getExpiryDate().toEpochMilli() - oldToken.getCreatedAt().toEpochMilli());
    }

    @Override
    public Optional<T> verifyToken(Long id, String token) {
        return ((BaseTokenRepository<T>)tokenRepository)
                .findByIdAndTokenAndRevokedFalse(id, token);
    }

    @Override
    @Transactional
    public T verifyExpiration(T token) {
        if (token.isRevoked()) {
            throw new TokenRefreshRevokedException("Token was revoked");
        }

        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            revokeToken(token);
            throw new TokenRefreshExpiredException("Token has expired");
        }

        return token;
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        ((BaseTokenRepository<T>)tokenRepository).revokeAllUserTokens(user);
    }

    @Override
    @Transactional
    public void revokeToken(T token) {
        token.setRevoked(true);
        tokenRepository.save(token);
    }

    protected String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
