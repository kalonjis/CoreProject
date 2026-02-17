package be.steby.CoreProject.il.ratelimit.config;

import be.steby.CoreProject.il.ratelimit.RateLimiter;
import be.steby.CoreProject.il.ratelimit.providers.InMemoryRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for rate limiting infrastructure.
 *
 * <p>This configuration class is responsible for instantiating the appropriate
 * {@link RateLimiter} implementation based on the configured provider in
 * application properties.</p>
 *
 * <h4>Provider Selection:</h4>
 * <p>The active provider is determined by the {@code rate-limit.provider} property:</p>
 * <ul>
 *   <li><b>memory</b> - {@link InMemoryRateLimiter} using Bucket4j (single instance)</li>
 *   <li><b>redis</b> - RedisRateLimiter using Redis backend (distributed, multi-instance)</li>
 * </ul>
 *
 * <h4>Configuration Example:</h4>
 * <pre>{@code
 * rate-limit:
 *   enabled: true
 *   provider: memory  # or "redis"
 *   requests-per-minute: 60
 *   burst-capacity: 100
 * }</pre>
 *
 * <h4>Conditional Bean Creation:</h4>
 * <p>Uses {@link ConditionalOnProperty} to ensure only one {@link RateLimiter}
 * bean is created based on configuration. This allows seamless switching between
 * providers without code changes.</p>
 *
 * <h4>Migration Path:</h4>
 * <p>To migrate from in-memory to Redis:</p>
 * <ol>
 *   <li>Add Redis dependencies to pom.xml</li>
 *   <li>Implement RedisRateLimiter class</li>
 *   <li>Add Redis connection properties</li>
 *   <li>Change {@code rate-limit.provider} to "redis"</li>
 *   <li>Restart application - no code changes needed</li>
 * </ol>
 *
 * @author Steby Core Team
 * @since 1.0.0
 * @see RateLimiter
 * @see RateLimitProperties
 * @see InMemoryRateLimiter
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RateLimitConfig {

    private final RateLimitProperties properties;

    /**
     * Creates an in-memory rate limiter using Bucket4j.
     *
     * <p>This bean is only created when {@code rate-limit.provider} is set to "memory"
     * (or not set, as "memory" is the default).</p>
     *
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Single-instance deployments</li>
     *   <li>Development and testing environments</li>
     *   <li>Small to medium applications without horizontal scaling</li>
     * </ul>
     *
     * <h4>Limitations:</h4>
     * <ul>
     *   <li>Rate limits are per-instance (not shared across multiple instances)</li>
     *   <li>Rate limit state is lost on application restart</li>
     *   <li>Cannot enforce global limits in clustered deployments</li>
     * </ul>
     *
     * @return InMemoryRateLimiter configured with application properties
     */
    @Bean
    @ConditionalOnProperty(name = "rate-limit.provider", havingValue = "memory", matchIfMissing = true)
    public RateLimiter inMemoryRateLimiter() {
        log.info("Initializing in-memory rate limiter with {} requests/min, burst capacity: {}",
                properties.getRequestsPerMinute(), properties.getBurstCapacity());
        
        return new InMemoryRateLimiter(
                properties.getRequestsPerMinute(),
                properties.getBurstCapacity()
        );
    }

    /**
     * Creates a Redis-backed rate limiter for distributed deployments.
     *
     * <p>This bean is only created when {@code rate-limit.provider} is set to "redis".</p>
     *
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Multi-instance deployments (horizontal scaling)</li>
     *   <li>Microservices architectures</li>
     *   <li>Applications requiring strict global rate limits</li>
     * </ul>
     *
     * <h4>Benefits:</h4>
     * <ul>
     *   <li>Rate limits shared across all application instances</li>
     *   <li>Rate limit state persists across restarts (if Redis is persistent)</li>
     *   <li>Enforces true global limits regardless of instance count</li>
     * </ul>
     *
     * <h4>Requirements:</h4>
     * <ul>
     *   <li>Redis server must be running and accessible</li>
     *   <li>Redis connection properties must be configured</li>
     *   <li>RedisRateLimiter implementation must be available</li>
     * </ul>
     *
     * <p><b>Note:</b> This method is currently commented out as RedisRateLimiter
     * is not yet implemented. Uncomment when Redis implementation is ready.</p>
     *
     * @return RedisRateLimiter configured with application properties
     */
    /*
    @Bean
    @ConditionalOnProperty(name = "rate-limit.provider", havingValue = "redis")
    public RateLimiter redisRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        log.info("Initializing Redis rate limiter with {} requests/min, burst capacity: {}",
                properties.getRequestsPerMinute(), properties.getBurstCapacity());
        
        return new RedisRateLimiter(
                redisTemplate,
                properties.getRequestsPerMinute(),
                properties.getBurstCapacity()
        );
    }
    */
}