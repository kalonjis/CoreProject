package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.dal.repositories.tokens.PasswordResetTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
public class PasswordResetTokenServiceImpl extends BaseTokenServiceImpl<PasswordResetToken> {

    @Value("${security.password-reset-token.expiration}")
    private Long passwordResetTokenDurationMs;

    private final PasswordResetAttemptServiceImpl attemptService;

    /**
     * Constructs a new {@link PasswordResetTokenServiceImpl} using the provided token repository.
     *
     * @param passwordResetTokenRepository The repository specifically for refresh tokens,
     *                              qualified to ensure correct repository injection
     */
    public PasswordResetTokenServiceImpl(
            @Qualifier("passwordResetTokenRepository")
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetAttemptServiceImpl attemptService) {
        super(passwordResetTokenRepository, PasswordResetToken.class);
        this.attemptService = attemptService;
    }

    /**
     * Creates a new refresh token for a user with the configured expiration time.
     *
     * @param user The user for whom to create the refresh token
     * @return The newly created refresh token
     */
    @Transactional
    public PasswordResetToken createPasswordResetToken(User user) {
        if (attemptService.hasExceededAttempts(user)) {
            throw new MaxAttemptsReachedException("Too many attempts. Please try again later.");
        }
        attemptService.recordAttempt(user);
        return super.createToken(user, passwordResetTokenDurationMs);
    }


    @Transactional
    public int getPasswordResetTokenDurationInSeconds() {
        long durationInSeconds = passwordResetTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("passwordReset token duration too large");
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
//        passwordResetTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Completed cleanup of expired tokens");
    }

}