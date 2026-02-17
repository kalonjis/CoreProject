package be.steby.CoreProject.il.ratelimit;

import be.steby.CoreProject.il.ratelimit.models.RateLimitResult;

/**
 * Core interface for rate limiting operations in the application.
 *
 * <p>This interface defines the contract for rate limiting implementations, allowing
 * the application to enforce request limits per user, IP address, or other identifiers.
 * The abstraction enables switching between different backends (in-memory, Redis, etc.)
 * without affecting consuming code.</p>
 *
 * <h4>Design Principles:</h4>
 * <ul>
 *   <li><b>Provider Agnostic:</b> Implementation can use any backend (memory, Redis, database)</li>
 *   <li><b>Single Responsibility:</b> Only handles token bucket/rate limit logic</li>
 *   <li><b>Immutable Results:</b> Returns result objects rather than modifying state directly</li>
 * </ul>
 *
 * <h4>Rate Limiting Strategy:</h4>
 * <p>This interface is designed around the <b>Token Bucket Algorithm</b>:</p>
 * <ul>
 *   <li>Each identifier (user/IP) has a "bucket" with a maximum capacity of tokens</li>
 *   <li>Each request consumes one token from the bucket</li>
 *   <li>Tokens are refilled at a constant rate (e.g., 60 tokens per minute)</li>
 *   <li>If no tokens are available, the request is rejected</li>
 * </ul>
 *
 * <h4>Available Implementations:</h4>
 * <ul>
 *   <li>{@link be.steby.CoreProject.il.ratelimit.providers.InMemoryRateLimiter} - 
 *       In-memory implementation using Bucket4j (for single-instance deployments)</li>
 *   <li>{@link be.steby.CoreProject.il.ratelimit.providers.RedisRateLimiter} - 
 *       Redis-backed implementation (for distributed/multi-instance deployments)</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // In RateLimitFilter
 * String key = "user:" + username; // or "ip:" + ipAddress
 * RateLimitResult result = rateLimiter.tryConsume(key);
 * 
 * if (!result.isAllowed()) {
 *     throw new RateLimitExceededException(
 *         "Too many requests",
 *         result.getRetryAfterSeconds()
 *     );
 * }
 * 
 * // Request allowed, continue processing
 * }</pre>
 *
 * <h4>Thread Safety:</h4>
 * <p>All implementations must be thread-safe as they will be accessed concurrently
 * by multiple HTTP request threads.</p>
 *
 * @author Steby Core Team
 * @since 1.0.0
 * @see RateLimitResult
 * @see be.steby.CoreProject.il.ratelimit.providers.InMemoryRateLimiter
 */
public interface RateLimiter {

    /**
     * Attempts to consume one token from the rate limit bucket for the given key.
     *
     * <p>This method checks if the identifier has available tokens and, if so,
     * consumes one token and returns a successful result. If no tokens are available,
     * returns a result indicating the request should be rejected.</p>
     *
     * <p><b>Key Format Conventions:</b></p>
     * <ul>
     *   <li>{@code "user:<username>"} - Rate limit per authenticated user</li>
     *   <li>{@code "ip:<ip_address>"} - Rate limit per IP address</li>
     *   <li>{@code "global"} - Global rate limit across all requests</li>
     * </ul>
     *
     * <p><b>Implementation Notes:</b></p>
     * <ul>
     *   <li>Must be thread-safe</li>
     *   <li>Must handle concurrent access to the same key</li>
     *   <li>Should efficiently handle many different keys</li>
     * </ul>
     *
     * @param key unique identifier for rate limiting (e.g., "user:john.doe" or "ip:192.168.1.1")
     * @return {@link RateLimitResult} containing whether the request is allowed and metadata
     * @throws IllegalArgumentException if key is null or empty
     */
    RateLimitResult tryConsume(String key);

    /**
     * Retrieves the current rate limit status for a given key without consuming a token.
     *
     * <p>This method is useful for checking how many requests a user/IP has remaining
     * without actually consuming a token. Can be used for metrics, debugging, or
     * informational purposes.</p>
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Exposing rate limit info in response headers (X-RateLimit-Remaining)</li>
     *   <li>Monitoring dashboards showing current rate limit usage</li>
     *   <li>Debugging rate limit issues</li>
     * </ul>
     *
     * @param key unique identifier for rate limiting
     * @return {@link RateLimitResult} with current status (tokens remaining, reset time)
     * @throws IllegalArgumentException if key is null or empty
     */
    RateLimitResult getStatus(String key);

    /**
     * Resets the rate limit for a specific key, refilling the bucket to full capacity.
     *
     * <p>This method is typically used for administrative purposes or testing.
     * In production, rate limits should naturally refill over time based on the
     * configured refill rate.</p>
     *
     * <p><b>Common Use Cases:</b></p>
     * <ul>
     *   <li>Admin tools to unblock a user who was rate limited</li>
     *   <li>Testing scenarios requiring clean state</li>
     *   <li>Clearing rate limits after a security incident is resolved</li>
     * </ul>
     *
     * <p><b>Security Warning:</b> This operation should be restricted to administrators
     * only, as it bypasses rate limiting protection.</p>
     *
     * @param key unique identifier for rate limiting
     * @throws IllegalArgumentException if key is null or empty
     */
    void reset(String key);
}