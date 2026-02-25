package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Generic repository for all token entities that extend {@link BaseToken}.
 *
 * <p>All queries use {@code #{#entityName}} so they resolve to the correct
 * concrete table at runtime (e.g. {@code AccountDeletionToken}, {@code RefreshToken}).
 *
 * @param <T> a token entity type extending {@link BaseToken}
 */
public interface BaseTokenRepository<T extends BaseToken> extends JpaRepository<T, Long> {

    // =========================================================================
    // SINGLE-FIELD LOOKUP
    // =========================================================================

    /**
     * Returns a token matching the given string value (case-insensitive).
     *
     * @param token the raw token string
     * @return the matching token, or empty if not found
     */
    Optional<T> findByTokenIgnoreCase(String token);

    /**
     * Returns the active (non-revoked) token for a given user.
     *
     * @param user the token owner
     * @return the active token, or empty if none exists
     */
    Optional<T> findByUserAndRevokedFalse(User user);

    /**
     * Returns a non-revoked token matching the given internal ID.
     *
     * @param id the token's database ID
     * @return the active token, or empty if not found or revoked
     */
    Optional<T> findByIdAndRevokedFalse(Long id);

    /**
     * Returns a non-revoked token matching both the internal ID and raw token string.
     *
     * @param id    the token's database ID
     * @param token the raw token string
     * @return the matching non-revoked token, or empty if not found
     */
    Optional<T> findByIdAndTokenAndRevokedFalse(Long id, String token);

    // =========================================================================
    // TYPE-AWARE LOOKUP
    // =========================================================================

    /**
     * Returns a token matching the given raw value and token type.
     *
     * @param token     the raw token string
     * @param tokenType the expected token type
     * @return the matching token, or empty if not found or type mismatch
     */
    Optional<T> findByTokenAndTokenType(String token, TokenType tokenType);

    /**
     * Returns a valid token matching the given raw value and type.
     * A token is valid if it is not revoked and has not yet expired.
     *
     * @param token     the raw token string
     * @param tokenType the expected token type
     * @param now       current timestamp used for expiry check
     * @return the valid token, or empty if not found, revoked, or expired
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.token = ?1 AND t.tokenType = ?2 AND t.revoked = false AND t.expiryDate > ?3")
    Optional<T> findValidTokenByTokenAndType(String token, TokenType tokenType, Instant now);

    /**
     * Returns the active (non-revoked) token for a given user and token type.
     *
     * @param user      the token owner
     * @param tokenType the token type to match
     * @return the active token, or empty if none exists
     */
    Optional<T> findByUserAndTokenTypeAndRevokedFalse(User user, TokenType tokenType);

    /**
     * Returns all tokens (active and inactive) for a given user and token type.
     *
     * @param user      the token owner
     * @param tokenType the token type to filter on
     * @return list of matching tokens, possibly empty
     */
    List<T> findAllByUserAndTokenType(User user, TokenType tokenType);

    /**
     * Returns all active (non-revoked) tokens of a given type across all users.
     *
     * @param tokenType the token type to filter on
     * @return list of active tokens, possibly empty
     */
    List<T> findAllByTokenTypeAndRevokedFalse(TokenType tokenType);

    // =========================================================================
    // REVOCATION
    // =========================================================================

    /**
     * Marks all tokens belonging to the given user as revoked.
     *
     * @param user the user whose tokens are revoked
     */
    @Modifying
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.user = ?1")
    void revokeAllUserTokens(User user);

    /**
     * Marks all tokens belonging to the given user (by ID) as revoked.
     * Returns the count of revoked tokens.
     *
     * @param userId the ID of the user whose tokens are revoked
     * @return number of tokens revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.user.id = :userId")
    int revokeAllTokensForUser(@Param("userId") Long userId);

    /**
     * Marks all tokens of the given user for a specific token type as revoked.
     *
     * @param user      the token owner
     * @param tokenType the token type to revoke
     */
    @Modifying
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.user = ?1 AND t.tokenType = ?2")
    void revokeAllUserTokensByType(User user, TokenType tokenType);

    /**
     * Marks all tokens of a specific type across all users as revoked.
     *
     * @param tokenType the token type to revoke globally
     */
    @Modifying
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.tokenType = ?1")
    void revokeAllTokensByType(TokenType tokenType);

    // =========================================================================
    // CLEANUP
    // =========================================================================

    /**
     * Deletes all tokens that are either expired or revoked.
     *
     * @param now current timestamp used to identify expired tokens
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} t WHERE t.expiryDate < ?1 OR t.revoked = true")
    void deleteExpiredTokens(Instant now);

    /**
     * Deletes all expired or revoked tokens of a specific type.
     * Returns the count of deleted tokens.
     *
     * @param tokenType the token type to clean up
     * @param now       current timestamp used to identify expired tokens
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} t WHERE t.tokenType = ?1 AND (t.expiryDate < ?2 OR t.revoked = true)")
    int deleteExpiredTokensByType(TokenType tokenType, Instant now);

    /**
     * Deletes all tokens of a specific type regardless of their status.
     *
     * @param tokenType the token type to delete entirely
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} t WHERE t.tokenType = ?1")
    int deleteAllByTokenType(TokenType tokenType);

    // =========================================================================
    // PUBLIC ID LOOKUP
    // =========================================================================

    /**
     * Returns a token matching the given public ID.
     *
     * @param publicId the encrypted public ID from URL
     * @return the matching token, or empty if not found
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.publicId = ?1")
    Optional<T> findByPublicId(String publicId);

    /**
     * Returns a token matching the given public ID and token type.
     *
     * @param publicId  the encrypted public ID from URL
     * @param tokenType the expected token type
     * @return the matching token, or empty if not found or type mismatch
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.publicId = ?1 AND t.tokenType = ?2")
    Optional<T> findByPublicIdAndTokenType(String publicId, TokenType tokenType);

    /**
     * Returns a non-revoked token matching both the internal ID and public ID.
     *
     * @param id       the token's database ID
     * @param publicId the encrypted public ID
     * @return the matching non-revoked token, or empty if not found
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.id = ?1 AND t.publicId = ?2 AND t.revoked = false")
    Optional<T> findByIdAndPublicIdAndRevokedFalse(Long id, String publicId);

    // =========================================================================
    // ANALYTICS
    // =========================================================================

    /**
     * Counts active (non-revoked, non-expired) tokens of a specific type.
     *
     * @param tokenType the token type to count
     * @param now       current timestamp used for expiry check
     * @return number of active tokens
     */
    @Query("SELECT COUNT(t) FROM #{#entityName} t WHERE t.tokenType = ?1 AND t.revoked = false AND t.expiryDate > ?2")
    long countActiveTokensByType(TokenType tokenType, Instant now);

    /**
     * Counts all tokens (active and inactive) of a specific type.
     *
     * @param tokenType the token type to count
     * @return total number of tokens of that type
     */
    long countByTokenType(TokenType tokenType);

    /**
     * Counts tokens of a specific type that are either expired or revoked.
     *
     * @param tokenType the token type to count
     * @param now       current timestamp used to identify expired tokens
     * @return number of expired or revoked tokens of that type
     */
    @Query("SELECT COUNT(t) FROM #{#entityName} t WHERE t.tokenType = ?1 AND (t.expiryDate < ?2 OR t.revoked = true)")
    long countExpiredTokensByType(TokenType tokenType, Instant now);
}