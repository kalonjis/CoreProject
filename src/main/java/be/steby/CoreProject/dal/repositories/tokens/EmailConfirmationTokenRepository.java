package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.tokens.EmailConfirmationToken;


/**
 * Repository for {@link EmailConfirmationToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByToken}, {@code revokeAllUserTokens}, {@code deleteExpiredTokens},
 * {@code findByIdAndTokenAndRevokedFalse} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only email-confirmation-specific queries are declared here.
 */
public interface EmailConfirmationTokenRepository extends BaseTokenRepository<EmailConfirmationToken> {}
