package be.steby.CoreProject.bll.domains.emailaddress.services.tokens;

import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.tokens.EmailConfirmationTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.EmailConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
public class EmailConfirmationTokenServiceImpl extends BaseTokenServiceImpl<EmailConfirmationToken> {

    @Value("${security.email-confirmation.token.expiration}")
    private Long emailConfirmationTokenDurationMs;

    private final EmailConfirmationAttemptServiceImpl attemptService;

    /**
     * Constructs a new {@link EmailConfirmationTokenServiceImpl} using the provided token repository.
     *
     * @param emailConfirmationTokenRepository The repository specifically for refresh tokens,
     *                              qualified to ensure correct repository injection
     */
    public EmailConfirmationTokenServiceImpl(
            @Qualifier("emailConfirmationTokenRepository") EmailConfirmationTokenRepository emailConfirmationTokenRepository,
            EmailConfirmationAttemptServiceImpl attemptService, SecureTokenService secureTokenService) {
        super(emailConfirmationTokenRepository, EmailConfirmationToken.class, secureTokenService);
        this.attemptService = attemptService;
    }

    /**
     * Creates a new refresh token for a user with the configured expiration time.
     *
     * @param user The user for whom to create the refresh token
     * @return The newly created refresh token
     */
    @Transactional
    public EmailConfirmationToken createEmailConfirmationToken(User user) {
        if (attemptService.hasExceededAttempts(user)) {
            throw new MaxAttemptsReachedException("Too many attempts. Please try again later.");
        }
        attemptService.recordAttempt(user);
        return super.createToken(user, TokenType.EMAIL_CONFIRMATION, emailConfirmationTokenDurationMs, true);
    }


    @Transactional
    public int getAccountConfirmationTokenDurationInSeconds() {
        long durationInSeconds = emailConfirmationTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("account-confirmation token duration too large");
        }
        return (int) durationInSeconds;
    }
}