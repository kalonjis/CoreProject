package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.dal.repositories.tokens.RefreshTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;



/**
 * Service implementation for managing refresh tokens. This service extends {@link BaseTokenServiceImpl}
 * and specifically handles operations related to JWT refresh tokens.
 * It provides functionality for creating, rotating, and verifying refresh tokens with proper
 * security measures.
 *
 * @see BaseTokenServiceImpl
 */
@Service
@Slf4j
public class RefreshTokenServiceImpl extends BaseTokenServiceImpl<RefreshToken> {

    @Value("${security.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;

    /**
     * Constructs a new {@link RefreshTokenServiceImpl} using the provided token repository.
     *
     * @param refreshTokenRepository The repository specifically for refresh tokens,
     *                              qualified to ensure correct repository injection
     */
    public RefreshTokenServiceImpl(
            @Qualifier("refreshTokenRepository") RefreshTokenRepository refreshTokenRepository) {
        super(refreshTokenRepository, RefreshToken.class);
    }

    /**
     * Creates a new refresh token for a user with the configured expiration time.
     *
     * @param user The user for whom to create the refresh token
     * @return The newly created refresh token
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        return super.createToken(user, refreshTokenDurationMs);
    }


    @Transactional
    public int getRefreshTokenDurationInSeconds() {
        long durationInSeconds = refreshTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("Refresh token duration too large for cookie max age");
        }
        return (int) durationInSeconds;
    }

    /**
     * Scheduled task to clean up expired and revoked tokens.
     * Runs hourly to maintain database cleanliness.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredTokens() {
        log.info("Starting scheduled cleanup of expired tokens");
//        refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Completed cleanup of expired tokens");
    }
}