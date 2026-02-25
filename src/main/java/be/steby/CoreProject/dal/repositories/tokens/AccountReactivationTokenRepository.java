package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.tokens.AccountReactivationToken;


/**
 * Repository for {@link AccountReactivationToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByToken}, {@code revokeAllUserTokens}, {@code deleteExpiredTokens},
 * {@code findByIdAndTokenAndRevokedFalse} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only reactivation-specific queries are declared here.
 */
public interface AccountReactivationTokenRepository extends BaseTokenRepository<AccountReactivationToken> {}