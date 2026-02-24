package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountDeletionToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for {@link AccountDeletionToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which provides all generic token
 * operations (find by token value, revoke by user, delete expired, etc.).
 * Only GDPR deletion-specific queries are defined here.
 *
 * <p>Key constraint enforced at the service layer:
 * a user can only have one active (non-revoked, non-expired) deletion token
 * at a time — checked via {@link #findByUserAndRevokedFalse(User)}.
 *
 * @see AccountDeletionToken
 * @see BaseTokenRepository
 */
public interface AccountDeletionTokenRepository extends BaseTokenRepository<AccountDeletionToken> {

    /**
     * Finds the active (non-revoked) deletion token for a given user.
     *
     * <p>Used to enforce the "one active deletion request per user" constraint
     * before creating a new token.
     *
     * @param user the user whose active token is looked up
     * @return the active token if one exists, empty otherwise
     */
    Optional<AccountDeletionToken> findByUserAndRevokedFalse(User user);

    /**
     * Finds a deletion token by its raw token value.
     *
     * <p>Used during confirmation — the token value is extracted from the
     * email link and looked up here before validation.
     *
     * @param token the raw token string from the confirmation link
     * @return the matching token if found, empty otherwise
     */
    Optional<AccountDeletionToken> findByToken(String token);

    /**
     * Revokes all active deletion tokens for a given user.
     *
     * <p>Called after successful deletion confirmation to invalidate
     * any remaining tokens, and on explicit cancellation.
     *
     * @param user the user whose tokens must be revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountDeletionToken t SET t.revoked = true WHERE t.user = :user")
    void revokeAllUserTokens(@Param("user") User user);

    /**
     * Deletes all expired or revoked deletion tokens.
     *
     * <p>Called by the scheduled {@link be.steby.CoreProject.bll.common.services.tokens.TokenCleanupService}
     * — no manual invocation needed.
     *
     * @param now current timestamp used to identify expired tokens
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccountDeletionToken t WHERE t.expiryDate < :now OR t.revoked = true")
    void deleteExpiredTokens(@Param("now") Instant now);
}