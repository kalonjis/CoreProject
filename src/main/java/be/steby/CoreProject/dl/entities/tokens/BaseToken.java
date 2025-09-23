package be.steby.CoreProject.dl.entities.tokens;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Base abstract entity for all security tokens in the application.
 *
 * This class provides common functionality for different types of tokens such as:
 * - Account confirmation tokens
 * - Password reset tokens
 * - Device confirmation tokens
 * - Account deactivation tokens
 * - etc.
 *
 * Security Features:
 * - Secure token generation (Base64 + SecureRandom)
 * - Automatic expiration handling
 * - Token revocation support
 * - User association for token ownership
 *
 * Database Strategy:
 * - Uses JOINED inheritance strategy for optimal performance
 * - Each token type gets its own table
 * - Shared fields are in the base token table
 *
 * Token Lifecycle:
 * 1. Created with expiration date
 * 2. Can be verified for validity
 * 3. Can be revoked manually
 * 4. Automatically expires after specified time
 * 5. Cleaned up by background jobs
 */
@Entity
@Table(name = "base_token", indexes = {
        @Index(name = "idx_token_value", columnList = "token"),
        @Index(name = "idx_token_user", columnList = "user_id"),
        @Index(name = "idx_token_expiry_revoked", columnList = "expiry_date, revoked")
})
@Inheritance(strategy = InheritanceType.JOINED)
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@NoArgsConstructor
public abstract class BaseToken extends BaseEntity<Long> {

    /**
     * The actual token value used for verification.
     *
     * - Generated using SecureRandom + Base64 encoding
     * - Unique across all token types
     * - Used in API endpoints and email links
     * - Acts as the "public ID" for tokens (no need for separate publicId)
     *
     * Example: "dGhpcyBpcyBhIHNlY3VyZSB0b2tlbg"
     */
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    /**
     * The user who owns this token.
     *
     * - Establishes token ownership
     * - Used for token management (revoke all user tokens)
     * - Eagerly fetched since tokens are always used with user context
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * When this token expires.
     *
     * - Set during token creation
     * - Used for automatic validation
     * - Background jobs clean up expired tokens
     * - Immutable once set
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Whether this token has been manually revoked.
     *
     * - Allows immediate invalidation without waiting for expiry
     * - Used for security (user logout, password change, etc.)
     * - Revoked tokens are kept for audit purposes
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    /**
     * Optional IP address from which the token was created.
     * Used for security tracking and suspicious activity detection.
     */
    @Column(name = "created_from_ip", length = 45)
    private String createdFromIp;

    /**
     * Optional user agent string from token creation request.
     * Used for security tracking and device identification.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * When this token was last used (optional tracking).
     * Updated when token is successfully verified.
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * How many times this token has been used (optional tracking).
     * Some tokens are single-use, others can be used multiple times.
     */
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor for creating tokens with basic information.
     *
     * @param user the user who owns this token
     * @param token the token value
     * @param expiryDate when the token expires
     */
    public BaseToken(User user, String token, Instant expiryDate) {
        this.user = user;
        this.token = token;
        this.expiryDate = expiryDate;
        this.revoked = false;
        this.usageCount = 0;
    }

    /**
     * Constructor with additional security context.
     *
     * @param user the user who owns this token
     * @param token the token value
     * @param expiryDate when the token expires
     * @param createdFromIp IP address where token was created
     * @param userAgent user agent string from creation request
     */
    public BaseToken(User user, String token, Instant expiryDate, String createdFromIp, String userAgent) {
        this(user, token, expiryDate);
        this.createdFromIp = createdFromIp;
        this.userAgent = userAgent;
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Checks if this token has expired.
     *
     * @return true if the current time is after the expiry date
     */
    public boolean isExpired() {
        return this.expiryDate != null && this.expiryDate.isBefore(Instant.now());
    }

    /**
     * Checks if this token is valid for use.
     *
     * A token is valid if it's:
     * - Not revoked
     * - Not expired
     *
     * @return true if the token can be used
     */
    public boolean isValid() {
        return !this.revoked && !isExpired();
    }

    /**
     * Checks if this token is invalid for use.
     *
     * @return true if the token cannot be used
     */
    public boolean isInvalid() {
        return !isValid();
    }

    // ==================== TOKEN MANAGEMENT ====================

    /**
     * Manually revokes this token.
     *
     * Once revoked, the token becomes permanently invalid.
     * Used for security purposes (logout, password change, etc.)
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * Records usage of this token.
     *
     * Updates last used timestamp and increments usage counter.
     * Call this method when the token is successfully verified.
     */
    public void recordUsage() {
        this.lastUsedAt = Instant.now();
        this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;
    }

    // ==================== TIME CALCULATIONS ====================

    /**
     * Gets the remaining time before token expiry.
     *
     * @return milliseconds until expiry, or 0 if already expired
     */
    public long getTimeUntilExpiryInMillis() {
        if (this.expiryDate == null) return 0L;

        long remaining = this.expiryDate.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }

    /**
     * Gets the remaining time before token expiry in seconds.
     *
     * @return seconds until expiry, or 0 if already expired
     */
    public long getTimeUntilExpiryInSeconds() {
        return getTimeUntilExpiryInMillis() / 1000L;
    }

    /**
     * Checks if this token will expire within the specified number of minutes.
     *
     * @param minutes the number of minutes to check
     * @return true if the token expires within the specified time
     */
    public boolean expiresWithin(long minutes) {
        return getTimeUntilExpiryInMillis() <= (minutes * 60 * 1000);
    }

    // ==================== SECURITY METHODS ====================

    /**
     * Checks if this token was created from the specified IP address.
     *
     * @param ipAddress the IP address to check
     * @return true if the token was created from the specified IP
     */
    public boolean isCreatedFromIp(String ipAddress) {
        return this.createdFromIp != null && this.createdFromIp.equals(ipAddress);
    }

    /**
     * Checks if this token was created with the specified user agent.
     *
     * @param userAgent the user agent to check
     * @return true if the token was created with the specified user agent
     */
    public boolean isCreatedWithUserAgent(String userAgent) {
        return this.userAgent != null && this.userAgent.equals(userAgent);
    }

    /**
     * Gets the number of times this token has been used.
     *
     * @return usage count, or 0 if never used
     */
    public int getUsageCount() {
        return this.usageCount != null ? this.usageCount : 0;
    }

    /**
     * Checks if this token has been used.
     *
     * @return true if usage count > 0
     */
    public boolean hasBeenUsed() {
        return getUsageCount() > 0;
    }

    /**
     * Checks if this is a single-use token that has already been used.
     * Override in subclasses if they are single-use tokens.
     *
     * @return true if this is a single-use token that has been used
     */
    public boolean isSingleUseAndUsed() {
        // Override in subclasses for single-use tokens
        return false;
    }

    // ==================== BASEENTITY OVERRIDE ====================

    /**
     * Tokens use their 'token' field as public identifier.
     * No need for separate publicId since token is already secure and unique.
     *
     * @return false to prevent publicId generation
     */
    @Override
    protected boolean shouldGeneratePublicId() {
        return false;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Returns a string representation of this token for logging.
     *
     * SECURITY NOTE: Only shows first/last few characters of actual token
     * to prevent accidental token exposure in logs.
     *
     * @return safe string representation
     */
    @Override
    public String toString() {
        String safeToken = this.token != null && this.token.length() > 8
                ? this.token.substring(0, 4) + "..." + this.token.substring(this.token.length() - 4)
                : "****";

        return String.format("%s{id=%d, token='%s', user='%s', expired=%s, revoked=%s}",
                getClass().getSimpleName(),
                getId(),
                safeToken,
                this.user != null ? this.user.getUsername() : "null",
                isExpired(),
                isRevoked()
        );
    }
}