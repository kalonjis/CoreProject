package be.steby.CoreProject.il.ratelimit.models;

import java.time.Instant;

/**
 * Immutable result object containing rate limiting decision and metadata.
 *
 * <p>This record encapsulates the outcome of a rate limit check, including whether
 * the request should be allowed and additional context such as remaining tokens
 * and when the limit will reset.</p>
 *
 * <h4>Design Principles:</h4>
 * <ul>
 *   <li><b>Immutable:</b> Uses Java 17 record for thread-safe immutability</li>
 *   <li><b>Self-Contained:</b> Contains all information needed to handle the rate limit decision</li>
 *   <li><b>API Response Ready:</b> Fields can be directly exposed in HTTP response headers</li>
 * </ul>
 *
 * <h4>HTTP Header Mapping:</h4>
 * <p>This result can be used to populate standard rate limit response headers:</p>
 * <ul>
 *   <li>{@code X-RateLimit-Limit} - Maximum requests allowed (capacity)</li>
 *   <li>{@code X-RateLimit-Remaining} - Tokens remaining before rate limit</li>
 *   <li>{@code X-RateLimit-Reset} - Unix timestamp when limit resets</li>
 *   <li>{@code Retry-After} - Seconds until client can retry (if not allowed)</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * RateLimitResult result = rateLimiter.tryConsume("user:john.doe");
 * 
 * if (result.allowed()) {
 *     // Add informational headers
 *     response.setHeader("X-RateLimit-Remaining", 
 *         String.valueOf(result.tokensRemaining()));
 *     response.setHeader("X-RateLimit-Reset", 
 *         String.valueOf(result.resetAt().getEpochSecond()));
 *     
 *     // Continue processing
 * } else {
 *     // Reject with 429 Too Many Requests
 *     response.setStatus(429);
 *     response.setHeader("Retry-After", 
 *         String.valueOf(result.retryAfterSeconds()));
 * }
 * }</pre>
 *
 * @param allowed whether the request should be allowed (true = proceed, false = reject)
 * @param tokensRemaining number of tokens left in the bucket (0 if not allowed)
 * @param capacity maximum capacity of the token bucket
 * @param resetAt timestamp when the bucket will be fully refilled
 * @param retryAfterSeconds seconds until next token is available (0 if allowed)
 *
 * @author Steby Core Team
 * @since 1.0.0
 * @see be.steby.CoreProject.il.ratelimit.RateLimiter
 */
public record RateLimitResult(
        boolean allowed,
        long tokensRemaining,
        long capacity,
        Instant resetAt,
        long retryAfterSeconds
) {

    /**
     * Creates a result indicating the request is allowed.
     *
     * <p>Factory method for successful rate limit checks. Sets {@code retryAfterSeconds}
     * to 0 since no retry is needed.</p>
     *
     * @param tokensRemaining number of tokens left after consumption
     * @param capacity maximum bucket capacity
     * @param resetAt when the bucket will be fully refilled
     * @return RateLimitResult with allowed=true
     */
    public static RateLimitResult allowed(long tokensRemaining, long capacity, Instant resetAt) {
        return new RateLimitResult(true, tokensRemaining, capacity, resetAt, 0);
    }

    /**
     * Creates a result indicating the request should be rejected.
     *
     * <p>Factory method for failed rate limit checks. Sets {@code tokensRemaining}
     * to 0 since the bucket is depleted.</p>
     *
     * @param capacity maximum bucket capacity
     * @param resetAt when the next token will be available
     * @param retryAfterSeconds seconds until the client can retry
     * @return RateLimitResult with allowed=false
     */
    public static RateLimitResult rejected(long capacity, Instant resetAt, long retryAfterSeconds) {
        return new RateLimitResult(false, 0, capacity, resetAt, retryAfterSeconds);
    }

    /**
     * Validates that the result object is in a consistent state.
     *
     * <p>Compact constructor enforces invariants:</p>
     * <ul>
     *   <li>If allowed, tokensRemaining must be ≥ 0 and retryAfterSeconds must be 0</li>
     *   <li>If not allowed, tokensRemaining must be 0 and retryAfterSeconds must be > 0</li>
     *   <li>Capacity must always be positive</li>
     *   <li>resetAt must not be null</li>
     * </ul>
     *
     * @throws IllegalArgumentException if any invariant is violated
     */
    public RateLimitResult {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        
        if (resetAt == null) {
            throw new IllegalArgumentException("Reset timestamp cannot be null");
        }
        
        if (allowed) {
            if (tokensRemaining < 0) {
                throw new IllegalArgumentException(
                    "Tokens remaining cannot be negative when allowed"
                );
            }
            if (retryAfterSeconds != 0) {
                throw new IllegalArgumentException(
                    "Retry after must be 0 when request is allowed"
                );
            }
        } else {
            if (tokensRemaining != 0) {
                throw new IllegalArgumentException(
                    "Tokens remaining must be 0 when not allowed"
                );
            }
            if (retryAfterSeconds <= 0) {
                throw new IllegalArgumentException(
                    "Retry after must be positive when request is rejected"
                );
            }
        }
    }

    /**
     * Checks if the request was allowed.
     *
     * <p>Convenience method that's more readable than accessing the field directly.</p>
     *
     * @return true if the request should proceed, false if it should be rejected
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * Checks if the rate limit has been exceeded.
     *
     * <p>Inverse of {@link #isAllowed()} for more intuitive conditional logic.</p>
     *
     * @return true if the rate limit was exceeded, false otherwise
     */
    public boolean isExceeded() {
        return !allowed;
    }

    /**
     * Gets the retry-after duration as seconds for HTTP Retry-After header.
     *
     * <p>This value is suitable for direct use in the {@code Retry-After} HTTP header.</p>
     *
     * @return seconds to wait before retrying (0 if request was allowed)
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}