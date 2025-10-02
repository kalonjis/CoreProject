package be.steby.CoreProject.bll.common.services.tokens;

import be.steby.CoreProject.dal.repositories.tokens.BaseTokenRepository;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global token service for operations that span across all token types.
 * Provides centralized token management when you need to work with all tokens
 * regardless of their specific type (refresh, confirmation, password reset, etc.).
 *
 * This service complements the type-specific services (RefreshTokenService, etc.)
 * and is particularly useful for security operations like revoking all tokens
 * when a user account is deactivated or compromised.
 */
@Service
@Slf4j
public class GlobalTokenServiceImpl extends BaseTokenServiceImpl<BaseToken> {

    private final BaseTokenRepository<BaseToken> baseTokenRepository;

    /**
     * Constructs the global token service with base token repository.
     *
     * @param baseTokenRepository Repository for all token types
     * @param secureTokenService Service for token encryption/decryption
     */
    public GlobalTokenServiceImpl(
            @Qualifier("baseTokenRepository") BaseTokenRepository<BaseToken> baseTokenRepository,
            SecureTokenService secureTokenService) {
        super(baseTokenRepository, BaseToken.class, secureTokenService);
        this.baseTokenRepository = baseTokenRepository;
    }

    /**
     * Revokes ALL tokens for a specific user across all token types.
     * This includes:
     * - Refresh tokens
     * - Device confirmation tokens
     * - Password reset tokens
     * - Email confirmation tokens
     * - Account activation/deactivation/reactivation tokens
     *
     * This method is particularly useful for security operations such as:
     * - Account deactivation
     * - Password compromise
     * - Security breach response
     * - Forced logout across all devices
     *
     * @param userId the ID of the user whose tokens should be revoked
     * @return total number of tokens revoked across all types
     */
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        log.info("Revoking all tokens for user ID: {}", userId);

        int revokedCount = revokeAllTokensForUser(userId);

        log.info("Successfully revoked {} tokens for user ID: {}", revokedCount, userId);
        return revokedCount;
    }
}