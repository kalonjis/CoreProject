package be.steby.CoreProject.bll.domains.account.services.tokens.reactivation;

import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.tokens.AccountReactivationTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountReactivationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * This service extends {@link BaseTokenServiceImpl}
 *
 * It provides functionality for creating, rotating, and verifying tokens with proper
 * security measures.
 *
 * @see BaseTokenServiceImpl
 */
@Service
@Slf4j
public class AccountReactivationTokenServiceImpl extends BaseTokenServiceImpl<AccountReactivationToken> {

    @Value("${security.account-reactivation.token.expiration}")
    private Long accountReactivationTokenDurationMs;

    private final AccountReactivationAttemptServiceImpl attemptService;

    /**
     * Constructs a new {@link AccountReactivationTokenServiceImpl} using the provided token repository.
     *
     * @param accountReactivationTokenRepository The repository specifically for refresh tokens,
     *                              qualified to ensure correct repository injection
     */
    public AccountReactivationTokenServiceImpl(
            @Qualifier("accountReactivationTokenRepository") AccountReactivationTokenRepository accountReactivationTokenRepository,
            AccountReactivationAttemptServiceImpl attemptService) {
        super(accountReactivationTokenRepository, AccountReactivationToken.class);
        this.attemptService = attemptService;
    }

    /**
     * Creates a new refresh token for a user with the configured expiration time.
     *
     * @param user The user for whom to create the refresh token
     * @return The newly created refresh token
     */
    @Transactional
    public AccountReactivationToken createAccountReactivationToken(User user) {
        if (attemptService.hasExceededAttempts(user)) {
            throw new MaxAttemptsReachedException("Too many attempts. Please try again later.");
        }
        attemptService.recordAttempt(user);
        return super.createToken(user, TokenType.ACCOUNT_REACTIVATION,  accountReactivationTokenDurationMs, true);
    }


    @Transactional
    public int getAccountReactivationTokenDurationInSeconds() {
        long durationInSeconds = accountReactivationTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("account reactivation token duration too large");
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