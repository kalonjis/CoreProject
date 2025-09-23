package be.steby.CoreProject.bll.domains.account.services.tokens.deactivation;

import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.DeactivationTokenExpiredException;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.DeactivationTokenInvalidException;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.DeactivationTokenNotFoundException;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.DeactivationTokenRevokedException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.tokens.AccountDeactivationTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountDeactivationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.dl.enums.DeactivationReason;
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
public class AccountDeactivationTokenServiceImpl extends BaseTokenServiceImpl<AccountDeactivationToken> {

    @Value("${security.account-deactivation.token.expiration}")
    private Long accountDeactivationTokenDurationMs;

    private final AccountDeactivationAttemptServiceImpl attemptService;

    /**
     * Constructs a new {@link AccountDeactivationTokenServiceImpl} using the provided token repository.
     *
     * @param accountDeactivationTokenRepository The repository specifically for refresh tokens,
     *                              qualified to ensure correct repository injection
     */
    public AccountDeactivationTokenServiceImpl(
            @Qualifier("accountDeactivationTokenRepository") AccountDeactivationTokenRepository accountDeactivationTokenRepository,
            AccountDeactivationAttemptServiceImpl attemptService) {
        super(accountDeactivationTokenRepository, AccountDeactivationToken.class);
        this.attemptService = attemptService;
    }

    @Override
    @Transactional
    public AccountDeactivationToken getToken(String token) {
        try {
            return super.getToken(token); // Délègue à la classe parent
        } catch (DoesntExistException e) {
            throw new DeactivationTokenNotFoundException("Deactivation token not found");
        }
    }

    /**
     * Creates a new refresh token for a user with the configured expiration time.
     *
     * @param user The user for whom to create the refresh token
     * @return The newly created refresh token
     */
    @Transactional
    public AccountDeactivationToken createAccountDeactivationToken(User user, DeactivationReason reason, String reasonDetails ) {
        if (attemptService.hasExceededAttempts(user)) {
            throw new MaxAttemptsReachedException("Too many attempts. Please try again later.");
        }
        attemptService.recordAttempt(user);

        AccountDeactivationToken token = super.createToken(user, TokenType.ACCOUNT_DEACTIVATION, accountDeactivationTokenDurationMs, true);
        token.setDeactivationReason(reason);
        token.setReasonDetails(reasonDetails);
        saveToken(token);
        return token;
    }


    @Override
    @Transactional
    public AccountDeactivationToken verifyTokenValidity(AccountDeactivationToken token){
        if (token == null) {
            throw new DeactivationTokenNotFoundException("Deactivation token not found");
        }

        if (token.isRevoked()) {
            throw new DeactivationTokenRevokedException("Token was revoked");
        }

        if (token.isExpired()) {
            throw new DeactivationTokenExpiredException("Token has expired");
        }

        return token;

    }


    @Transactional
    public int getAccountDeactivationTokenDurationInSeconds() {
        long durationInSeconds = accountDeactivationTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("account deactivation token duration too large");
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