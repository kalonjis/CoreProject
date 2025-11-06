package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.SmsToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for managing SMS password reset tokens.
 * 
 * <p>This repository extends {@link BaseTokenRepository} to inherit comprehensive token
 * management capabilities and adds SMS-specific operations for rate limiting,
 * code validation, and security features.</p>
 * 
 * <h4>Key Features:</h4>
 * <ul>
 *   <li>Rate limiting: Prevents SMS spam and brute force attacks</li>
 *   <li>Code validation: Secure verification code lookup and validation</li>
 *   <li>Security audit: Tracking of SMS token usage patterns</li>
 *   <li>Automatic cleanup: Removal of expired and revoked tokens</li>
 * </ul>
 * 
 * <h4>Security Considerations:</h4>
 * <ul>
 *   <li>All verification codes are stored as hashes, never in plain text</li>
 *   <li>Rate limiting prevents abuse of SMS services</li>
 *   <li>Tokens are automatically cleaned up to prevent database bloat</li>
 * </ul>
 * 
 * @see BaseTokenRepository
 * @see SmsToken
 */
public interface SmsTokenRepository extends BaseTokenRepository<SmsToken> {

    // ========== CORE TOKEN LOOKUP METHODS ==========

    /**
     * Finds an active SMS password reset token by its unique token identifier.
     * 
     * <p>This method looks up a token by its UUID and ensures it's still valid
     * (not revoked). Used during SMS code validation process.</p>
     * 
     * @param token the unique token identifier (UUID)
     * @return Optional containing the token if found and not revoked
     */
    Optional<SmsToken> findByTokenAndRevokedFalse(String token);

    /**
     * Finds the active SMS password reset token for a specific user.
     * 
     * <p>Returns the current active token for a user. Since a user should only
     * have one active SMS reset token at a time, this helps enforce that constraint.</p>
     * 
     * @param user the user whose active token to find
     * @return Optional containing the active token if found
     */
    Optional<SmsToken> findByUserAndRevokedFalse(User user);

    /**
     * Finds a valid (non-revoked, non-expired) SMS token by token identifier.
     * 
     * <p>Combines token lookup with expiry validation in a single database query
     * for optimal performance during code validation.</p>
     * 
     * @param token the unique token identifier
     * @param now current timestamp for expiry validation
     * @return Optional containing the token if valid
     */
    @Query("SELECT t FROM SmsToken t WHERE t.token = :token AND t.revoked = false AND t.expiryDate > :now")
    Optional<SmsToken> findValidByToken(@Param("token") String token, @Param("now") Instant now);

    // ========== RATE LIMITING METHODS ==========

    /**
     * Counts SMS password reset tokens created by a user within a time window.
     * 
     * <p>This method is crucial for rate limiting SMS requests. It counts all tokens
     * (regardless of their revoked status) created by a user within the specified
     * time frame to prevent SMS spam and abuse.</p>
     * 
     * <p><strong>Rate Limiting Logic:</strong> Used to enforce rules like 
     * "maximum 3 SMS requests per 15 minutes per user".</p>
     * 
     * @param user the user to check
     * @param since the start of the time window to check
     * @return number of SMS tokens created since the given time
     */
    @Query("SELECT COUNT(t) FROM SmsToken t WHERE t.user = :user AND t.createdAt >= :since")
    long countByUserAndCreatedAtAfter(@Param("user") User user, @Param("since") Instant since);

    /**
     * Counts failed SMS verification attempts by a user within a time window.
     * 
     * <p>This method helps implement brute force protection by tracking how many
     * times a user has attempted to validate SMS codes. Can be used to temporarily
     * lock out users who make too many incorrect attempts.</p>
     * 
     * <p><strong>Security Note:</strong> This counts actual validation attempts,
     * not just token creation, providing more granular rate limiting.</p>
     * 
     * @param user the user to check
     * @param since the start of the time window to check  
     * @return number of revoked tokens (indicating failed attempts) since the given time
     */
    @Query("SELECT COUNT(t) FROM SmsToken t WHERE t.user = :user AND t.revoked = true AND t.updatedAt >= :since")
    long countFailedAttemptsByUserSince(@Param("user") User user, @Param("since") Instant since);

    // ========== TOKEN MANAGEMENT METHODS ==========

    /**
     * Revokes all SMS password reset tokens for a specific user.
     * 
     * <p>This method is called when creating a new SMS token to ensure only one
     * active token exists per user, or when a user successfully resets their password.</p>
     * 
     * @param user the user whose SMS tokens should be revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SmsToken t SET t.revoked = true WHERE t.user = :user")
    void revokeAllUserSmsTokens(@Param("user") User user);

    /**
     * Deletes expired and revoked SMS password reset tokens.
     * 
     * <p>This cleanup method removes tokens that are no longer needed:
     * expired tokens and successfully used (revoked) tokens. Scheduled
     * to run periodically to prevent database bloat.</p>
     * 
     * <p><strong>Cleanup Strategy:</strong> Remove tokens that are either expired
     * OR revoked to maintain optimal database performance.</p>
     * 
     * @param now current timestamp for expiry comparison
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SmsToken t WHERE t.expiryDate < :now OR t.revoked = true")
    void deleteExpiredAndRevokedTokens(@Param("now") Instant now);

    // ========== ANALYTICS AND MONITORING METHODS ==========

    /**
     * Counts total active SMS password reset tokens across all users.
     * 
     * <p>Provides system-wide metrics for monitoring SMS token usage patterns
     * and system load. Useful for operational dashboards and capacity planning.</p>
     * 
     * @param now current timestamp for expiry validation
     * @return number of currently active SMS tokens system-wide
     */
    @Query("SELECT COUNT(t) FROM SmsToken t WHERE t.revoked = false AND t.expiryDate > :now")
    long countActiveTokens(@Param("now") Instant now);

    /**
     * Counts SMS tokens created within a specific time period.
     * 
     * <p>Provides insights into SMS usage patterns and helps identify potential
     * abuse or unusual activity spikes. Useful for security monitoring and
     * SMS service cost tracking.</p>
     * 
     * @param startTime start of the period to analyze
     * @param endTime end of the period to analyze
     * @return number of SMS tokens created in the specified period
     */
    @Query("SELECT COUNT(t) FROM SmsToken t WHERE t.createdAt BETWEEN :startTime AND :endTime")
    long countTokensCreatedBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}