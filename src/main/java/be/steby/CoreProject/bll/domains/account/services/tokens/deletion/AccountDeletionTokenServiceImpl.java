package be.steby.CoreProject.bll.domains.account.services.tokens.deletion;

import be.steby.CoreProject.bll.common.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.dal.repositories.tokens.AccountDeletionTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountDeletionToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service responsible for creating, validating, and revoking
 * {@link AccountDeletionToken} instances used in the GDPR deletion flow.
 *
 * <p>Extends {@link BaseTokenServiceImpl} which handles the generic
 * token lifecycle (secure generation, expiry, revocation).
 * Only deletion-specific behaviour is defined here.
 *
 * <p>Configuration property:
 * <ul>
 *   <li>{@code security.account-deletion.token.expiration} — token TTL in milliseconds</li>
 * </ul>
 */
@Service
@Slf4j
public class AccountDeletionTokenServiceImpl extends BaseTokenServiceImpl<AccountDeletionToken> {

    @Value("${security.account-deletion.token.expiration}")
    private Long tokenDurationMs;

    private final AccountDeletionAttemptServiceImpl attemptService;

    public AccountDeletionTokenServiceImpl(
            @Qualifier("accountDeletionTokenRepository") AccountDeletionTokenRepository deletionTokenRepository,
            AccountDeletionAttemptServiceImpl attemptService,
            SecureTokenService secureTokenService) {
        super(deletionTokenRepository, AccountDeletionToken.class, secureTokenService);
        this.attemptService = attemptService;
    }

    // =========================================================================
    // Token creation
    // =========================================================================

    /**
     * Creates a new single-use deletion confirmation token for the given user.
     *
     * <p>Enforces attempt rate limiting before creation.
     *
     * @param user the user requesting GDPR deletion
     * @return the created token
     * @throws MaxAttemptsReachedException if the user has exceeded the allowed attempts
     */
    @Transactional
    public AccountDeletionToken createAccountDeletionToken(User user) {
        if (attemptService.hasExceededAttempts(user)) {
            throw new MaxAttemptsReachedException(
                    "Too many deletion requests. Please try again later.");
        }
        attemptService.recordAttempt(user);

        return super.createToken(user, TokenType.ACCOUNT_DELETION, tokenDurationMs, true);
    }

    @Transactional
    public int getAccountConfirmationTokenDurationInSeconds() {
        long durationInSeconds = tokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("account deletion token duration too large");
        }
        return (int) durationInSeconds;
    }

}