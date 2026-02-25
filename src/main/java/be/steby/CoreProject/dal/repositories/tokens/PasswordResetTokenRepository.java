package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;

/**
 * Repository for {@link PasswordResetToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByToken}, {@code revokeAllUserTokens}, {@code deleteExpiredTokens},
 * {@code findByIdAndTokenAndRevokedFalse} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only password-reset-specific queries are declared here.
 */
public interface PasswordResetTokenRepository extends BaseTokenRepository<PasswordResetToken>{}