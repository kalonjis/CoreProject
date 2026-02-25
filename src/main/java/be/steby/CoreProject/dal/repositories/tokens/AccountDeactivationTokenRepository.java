package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.tokens.AccountDeactivationToken;


/**
 * Repository for {@link AccountDeactivationToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByToken}, {@code revokeAllUserTokens}, {@code deleteExpiredTokens},
 * {@code findByIdAndTokenAndRevokedFalse} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only deactivation-specific queries are declared here.
 */
public interface AccountDeactivationTokenRepository extends BaseTokenRepository<AccountDeactivationToken> {}