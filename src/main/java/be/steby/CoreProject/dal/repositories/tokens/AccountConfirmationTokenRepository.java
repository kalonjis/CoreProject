package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;


/**
 * Repository for {@link AccountConfirmationToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByToken}, {@code revokeAllUserTokens}, {@code deleteExpiredTokens},
 * {@code findByIdAndTokenAndRevokedFalse} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only confirmation-specific queries are declared here.
 */
public interface AccountConfirmationTokenRepository extends BaseTokenRepository<AccountConfirmationToken> {}