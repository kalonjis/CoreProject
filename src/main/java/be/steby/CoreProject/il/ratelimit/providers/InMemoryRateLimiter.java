package be.steby.CoreProject.il.ratelimit.providers;

import be.steby.CoreProject.il.ratelimit.RateLimiter;
import be.steby.CoreProject.il.ratelimit.models.RateLimitResult;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter implementation using Bucket4j token bucket algorithm.
 *
 * <p>This implementation stores rate limit state in application memory using a
 * thread-safe {@link ConcurrentHashMap}. Each unique key (user/IP) gets its own
 * token bucket that refills at a configured rate.</p>
 *
 * <h4>Token Bucket Algorithm:</h4>
 * <ul>
 *   <li><b>Capacity:</b> Maximum tokens a bucket can hold (burst capacity)</li>
 *   <li><b>Refill Rate:</b> Tokens added per time period (e.g., 60/minute)</li>
 *   <li><b>Consumption:</b> Each request consumes 1 token</li>
 *   <li><b>Rejection:</b> Request rejected if no tokens available</li>
 * </ul>
 *
 * <h4>Advantages:</h4>
 * <ul>
 *   <li>No external dependencies (Redis, database)</li>
 *   <li>Very fast - all operations in memory</li>
 *   <li>Simple to configure and deploy</li>
 *   <li>Suitable for single-instance applications</li>
 * </ul>
 *
 * <h4>Limitations:</h4>
 * <ul>
 *   <li><b>Single Instance Only:</b> Does not work across multiple application instances</li>
 *   <li><b>No Persistence:</b> Rate limit state lost on restart</li>
 *   <li><b>Memory Usage:</b> Stores bucket for every unique user/IP</li>
 * </ul>
 *
 * <h4>Memory Management:</h4>
 * <p>To prevent unbounded memory growth, a scheduled task runs every 5 minutes
 * to clear the cache. This is a simple approach - production systems might implement
 * more sophisticated eviction policies (LRU, TTL, etc.).</p>
 *
 * <h4>Thread Safety:</h4>
 * <p>This implementation is fully thread-safe:</p>
 * <ul>
 *   <li>{@link ConcurrentHashMap} provides thread-safe map operations</li>
 *   <li>Bucket4j buckets are thread-safe internally</li>
 *   <li>{@code computeIfAbsent} ensures atomic bucket creation</li>
 * </ul>
 *
 * <h4>Configuration Example:</h4>
 * <pre>{@code
 * // 60 requests per minute, burst capacity of 100
 * InMemoryRateLimiter limiter = new InMemoryRateLimiter(60, 100);
 * 
 * // This allows:
 * // - Steady state: 1 request/second (60/min)
 * // - Burst: Up to 100 requests if bucket is full
 * // - Recovery: Refills at 1 token/second
 * }</pre>
 *
 * @author Steby Core Team
 * @since 1.0.0
 * @see RateLimiter
 * @see Bucket
 * @see Bandwidth
 */
@Slf4j
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final int burstCapacity;
    private final Bandwidth bandwidth;

    /**
     * Creates a new in-memory rate limiter with specified limits.
     *
     * @param requestsPerMinute number of tokens refilled per minute (refill rate)
     * @param burstCapacity maximum tokens that can accumulate (bucket capacity)
     */
    public InMemoryRateLimiter(int requestsPerMinute, int burstCapacity) {
        this.requestsPerMinute = requestsPerMinute;
        this.burstCapacity = burstCapacity;
        
        this.bandwidth = Bandwidth.classic(
                burstCapacity,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        
        log.info("InMemoryRateLimiter initialized: {} requests/min, burst capacity: {}",
                requestsPerMinute, burstCapacity);
    }

    @Override
    public RateLimitResult tryConsume(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Rate limit key cannot be null or empty");
        }

        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket());
        
        if (bucket.tryConsume(1)) {
            long remaining = bucket.getAvailableTokens();
            Instant resetAt = calculateResetTime();
            
            log.trace("Rate limit check passed for key: {} (remaining: {})", key, remaining);
            return RateLimitResult.allowed(remaining, burstCapacity, resetAt);
        } else {
            long nanosUntilRefill = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            long secondsUntilRefill = Duration.ofNanos(nanosUntilRefill).getSeconds() + 1;
            Instant resetAt = Instant.now().plusSeconds(secondsUntilRefill);
            
            log.debug("Rate limit exceeded for key: {} (retry after: {}s)", key, secondsUntilRefill);
            return RateLimitResult.rejected(burstCapacity, resetAt, secondsUntilRefill);
        }
    }

    @Override
    public RateLimitResult getStatus(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Rate limit key cannot be null or empty");
        }

        Bucket bucket = buckets.get(key);
        
        if (bucket == null) {
            Instant resetAt = calculateResetTime();
            return RateLimitResult.allowed(burstCapacity, burstCapacity, resetAt);
        }

        long remaining = bucket.getAvailableTokens();
        Instant resetAt = calculateResetTime();
        
        return RateLimitResult.allowed(remaining, burstCapacity, resetAt);
    }

    @Override
    public void reset(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Rate limit key cannot be null or empty");
        }

        buckets.remove(key);
        log.info("Rate limit reset for key: {}", key);
    }

    /**
     * Creates a new token bucket with configured capacity and refill rate.
     *
     * @return new Bucket instance
     */
    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Calculates when the bucket will be fully refilled.
     *
     * @return Instant representing the reset time
     */
    private Instant calculateResetTime() {
        return Instant.now().plusSeconds(60);
    }

    /**
     * Periodic cleanup task to prevent unbounded memory growth.
     *
     * <p>Clears all cached buckets every 5 minutes. This is a simple approach
     * that works well for most use cases. For production systems with high traffic,
     * consider implementing:</p>
     * <ul>
     *   <li>LRU eviction policy</li>
     *   <li>TTL-based expiration</li>
     *   <li>Size-based limits</li>
     * </ul>
     *
     * <p><b>Note:</b> This clears rate limit state for all users. In practice,
     * this means users get their limits "reset" every 5 minutes, which may or
     * may not be desired behavior.</p>
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupCache() {
        int size = buckets.size();
        buckets.clear();
        log.debug("Rate limit cache cleaned up ({} buckets removed)", size);
    }
}