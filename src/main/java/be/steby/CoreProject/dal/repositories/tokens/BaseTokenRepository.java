package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Base repository interface for token entities extending BaseToken.
 * Provides comprehensive CRUD operations, security features, and analytics
 * for all token types in the application.
 *
 * @param <T> Token entity type extending BaseToken
 */
public interface BaseTokenRepository<T extends BaseToken> extends JpaRepository<T, Long> {

    // ========== LEGACY METHODS (for backward compatibility) ==========

    /**
     * Finds a token by its string value (case-insensitive).
     * @param token the token string to search for
     * @return Optional containing the token if found
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.token ilike :token")
    Optional<T> findByToken(String token);

    /**
     * Finds a token by ID, token value, and ensures it's not revoked.
     * @param id token ID
     * @param token token string value
     * @return Optional containing the token if found and valid
     */
    Optional<T> findByIdAndTokenAndRevokedFalse(Long id, String token);

    /**
     * Revokes all tokens belonging to a specific user.
     * @param user the user whose tokens should be revoked
     */
    @Modifying
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.user = ?1")
    void revokeAllUserTokens(User user);

    /**
     * Deletes all expired or revoked tokens.
     * @param now current timestamp for expiry comparison
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} t WHERE t.expiryDate < ?1 OR t.revoked = true")
    void deleteExpiredTokens(Instant now);

    // ========== TOKEN TYPE-AWARE SEARCH METHODS ==========

    /**
     * Finds a token by its value and type for enhanced security.
     * Prevents cross-type token usage vulnerabilities.
     * @param token the token string value
     * @param tokenType the expected token type
     * @return Optional containing the token if found and type matches
     */
    Optional<T> findByTokenAndTokenType(String token, TokenType tokenType);

    /**
     * Finds a valid (non-revoked, non-expired) token by value and type.
     * Combines token lookup with validation in a single database query.
     * @param token the token string value
     * @param tokenType the expected token type
     * @param now current timestamp for expiry validation
     * @return Optional containing the token if valid
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.token = ?1 AND t.tokenType = ?2 AND t.revoked = false AND t.expiryDate > ?3")
    Optional<T> findValidTokenByTokenAndType(String token, TokenType tokenType, Instant now);

    /**
     * Finds the active token of a user for a specific token type.
     * Useful for ensuring one active token per type per user.
     * @param user the token owner
     * @param tokenType the token type to search for
     * @return Optional containing the active token if found
     */
    Optional<T> findByUserAndTokenTypeAndRevokedFalse(User user, TokenType tokenType);

    /**
     * Finds all tokens (active and inactive) of a user for a specific type.
     * @param user the token owner
     * @param tokenType the token type to search for
     * @return List of matching tokens
     */
    List<T> findAllByUserAndTokenType(User user, TokenType tokenType);

    /**
     * Finds all active tokens of a specific type across all users.
     * @param tokenType the token type to search for
     * @return List of active tokens
     */
    List<T> findAllByTokenTypeAndRevokedFalse(TokenType tokenType);

    // ========== TOKEN TYPE-AWARE MANAGEMENT METHODS ==========

    /**
     * Revokes all tokens of a specific user for a given token type.
     * More granular than revoking all user tokens.
     * @param user the token owner
     * @param tokenType the token type to revoke
     */
    @Modifying
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.user = ?1 AND t.tokenType = ?2")
    void revokeAllUserTokensByType(User user, TokenType tokenType);

    /**
     * Revokes all tokens of a specific type across all users.
     * Useful for emergency security measures or type deprecation.
     * @param tokenType the token type to revoke globally
     */
    @Modifying
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.tokenType = ?1")
    void revokeAllTokensByType(TokenType tokenType);

    // ========== TOKEN TYPE-AWARE CLEANUP METHODS ==========

    /**
     * Deletes expired or revoked tokens of a specific type.
     * Returns count for monitoring and logging purposes.
     * @param tokenType the token type to clean up
     * @param now current timestamp for expiry comparison
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} t WHERE t.tokenType = ?1 AND (t.expiryDate < ?2 OR t.revoked = true)")
    int deleteExpiredTokensByType(TokenType tokenType, Instant now);

    /**
     * Deletes all tokens of a specific type regardless of status.
     * Use with caution - primarily for migrations or complete type removal.
     * @param tokenType the token type to delete completely
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} t WHERE t.tokenType = ?1")
    int deleteAllByTokenType(TokenType tokenType);

    // ========== ANALYTICS AND MONITORING METHODS ==========

    /**
     * Counts active tokens of a specific type.
     * Active means not revoked and not expired.
     * @param tokenType the token type to count
     * @param now current timestamp for expiry validation
     * @return number of active tokens
     */
    @Query("SELECT COUNT(t) FROM #{#entityName} t WHERE t.tokenType = ?1 AND t.revoked = false AND t.expiryDate > ?2")
    long countActiveTokensByType(TokenType tokenType, Instant now);

    /**
     * Counts all tokens of a specific type (active and inactive).
     * @param tokenType the token type to count
     * @return total number of tokens
     */
    long countByTokenType(TokenType tokenType);

    /**
     * Provides comprehensive token statistics grouped by type.
     * Returns raw data for dashboard and monitoring systems.
     * @return List of arrays where each array contains [TokenType, Count]
     */
    @Query("SELECT t.tokenType, COUNT(t) FROM #{#entityName} t GROUP BY t.tokenType")
    List<Object[]> getTokenStatsByType();

    /**
     * Provides active token statistics grouped by type.
     * Excludes revoked and expired tokens from counts.
     * @param now current timestamp for expiry validation
     * @return List of arrays where each array contains [TokenType, ActiveCount]
     */
    @Query("SELECT t.tokenType, COUNT(t) FROM #{#entityName} t WHERE t.revoked = false AND t.expiryDate > ?1 GROUP BY t.tokenType")
    List<Object[]> getActiveTokenStatsByType(Instant now);

    // ========== ADVANCED ANALYTICS AND AUDIT METHODS ==========

    /**
     * Finds tokens expiring within a specific time window.
     * Useful for proactive renewal notifications or cleanup scheduling.
     * @param tokenType the token type to check
     * @param startDate start of the time window
     * @param endDate end of the time window
     * @return List of tokens expiring in the specified period
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.tokenType = ?1 AND t.revoked = false AND t.expiryDate BETWEEN ?2 AND ?3")
    List<T> findTokensExpiringBetween(TokenType tokenType, Instant startDate, Instant endDate);

    /**
     * Finds the oldest active tokens of a specific type.
     * Useful for audit purposes and identifying long-lived tokens.
     * @param tokenType the token type to analyze
     * @return List of tokens ordered by creation date (oldest first)
     */
    @Query("SELECT t FROM #{#entityName} t WHERE t.tokenType = ?1 AND t.revoked = false ORDER BY t.createdAt ASC")
    List<T> findOldestTokensByType(TokenType tokenType);

    /**
     * Counts distinct users with active tokens of a specific type.
     * Useful for user engagement metrics and feature adoption tracking.
     * @param tokenType the token type to analyze
     * @param now current timestamp for expiry validation
     * @return number of unique users with active tokens of this type
     */
    @Query("SELECT COUNT(DISTINCT t.user.id) FROM #{#entityName} t WHERE t.tokenType = ?1 AND t.revoked = false AND t.expiryDate > ?2")
    long countDistinctActiveUsersByTokenType(TokenType tokenType, Instant now);

    // Méthode manquante pour les statistiques
    @Query("SELECT COUNT(t) FROM #{#entityName} t WHERE t.tokenType = ?1 AND (t.expiryDate < ?2 OR t.revoked = true)")
    long countExpiredTokensByType(TokenType tokenType, Instant now);

    // ========== NEW PUBLIC_ID METHODS (added for URL security) ==========

    /**
     * Finds a token by its public_id (encrypted token for URLs).
     * @param publicId the encrypted public_id from URL
     * @return Optional containing the token if found
     */
    Optional<T> findByPublicId(String publicId);

    /**
     * Finds a token by public_id and type for enhanced security.
     * @param publicId the encrypted public_id from URL
     * @param tokenType the expected token type
     * @return Optional containing the token if found and type matches
     */
    Optional<T> findByPublicIdAndTokenType(String publicId, TokenType tokenType);

    /**
     * Finds a token by ID, public_id, and ensures it's not revoked.
     * @param id token ID
     * @param publicId encrypted public_id string value
     * @return Optional containing the token if found and valid
     */
    Optional<T> findByIdAndPublicIdAndRevokedFalse(Long id, String publicId);
}