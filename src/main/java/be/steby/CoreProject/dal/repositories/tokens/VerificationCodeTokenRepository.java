package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.VerificationCodeToken;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for {@link VerificationCodeToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByTokenIgnoreCase}, {@code findByUserAndRevokedFalse},
 * {@code findByIdAndRevokedFalse}, {@code revokeAllUserTokens},
 * {@code deleteExpiredTokens} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only verification-code-specific queries are declared here.
 */
public interface VerificationCodeTokenRepository extends BaseTokenRepository<VerificationCodeToken> {

    // =========================================================================
    // VERIFICATION CODE LOOKUP
    // =========================================================================

    /**
     * Returns a non-revoked verification code token matching the given raw token string.
     *
     * @param token the raw token string
     * @return the active token if found, empty otherwise
     */
    Optional<VerificationCodeToken> findByTokenAndRevokedFalse(String token);

    /**
     * Returns a valid (non-revoked, non-expired) verification code token
     * matching the given raw token string.
     *
     * @param token the raw token string
     * @param now   current timestamp used for expiry check
     * @return the valid token if found, empty otherwise
     */
    @Query("SELECT t FROM VerificationCodeToken t WHERE t.token = :token AND t.revoked = false AND t.expiryDate > :now")
    Optional<VerificationCodeToken> findValidByToken(@Param("token") String token, @Param("now") Instant now);

    // =========================================================================
    // RATE LIMITING
    // =========================================================================

    /**
     * Counts verification code tokens created by a user after the given timestamp.
     * Includes both active and revoked tokens.
     *
     * @param user  the user to check
     * @param since the start of the time window
     * @return number of tokens created since the given time
     */
    @Query("SELECT COUNT(t) FROM VerificationCodeToken t WHERE t.user = :user AND t.createdAt >= :since")
    long countByUserAndCreatedAtAfter(@Param("user") User user, @Param("since") Instant since);

    /**
     * Counts revoked verification code tokens for a user updated after the given timestamp.
     * A revoked token indicates a failed or consumed verification attempt.
     *
     * @param user  the user to check
     * @param since the start of the time window
     * @return number of failed attempts since the given time
     */
    @Query("SELECT COUNT(t) FROM VerificationCodeToken t WHERE t.user = :user AND t.revoked = true AND t.updatedAt >= :since")
    long countFailedAttemptsByUserSince(@Param("user") User user, @Param("since") Instant since);
}