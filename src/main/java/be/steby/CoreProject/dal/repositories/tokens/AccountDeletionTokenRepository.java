package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.tokens.AccountDeletionToken;

import java.util.Optional;

/**
 * Repository for {@link AccountDeletionToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByToken}, {@code revokeAllUserTokens}, {@code deleteExpiredTokens},
 * {@code findByIdAndTokenAndRevokedFalse} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only the GDPR deletion-specific query is declared here.
 */
public interface AccountDeletionTokenRepository extends BaseTokenRepository<AccountDeletionToken> {}