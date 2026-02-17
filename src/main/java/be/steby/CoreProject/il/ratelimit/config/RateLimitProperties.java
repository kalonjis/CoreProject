package be.steby.CoreProject.il.ratelimit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for global API rate limiting.
 *
 * <p>This class binds to the {@code rate-limit} prefix in application.yml/properties,
 * providing type-safe access to rate limiting configuration values.</p>
 *
 * <h4>Configuration Example:</h4>
 * <pre>{@code
 * rate-limit:
 *   enabled: true
 *   provider: memory
 *   requests-per-minute: 60
 *   burst-capacity: 100
 *   exclude-paths:
 *     - /actuator/**
 *     - /swagger-ui/**
 *     - /api/public/**
 * }</pre>
 *
 * <h4>Provider Types:</h4>
 * <ul>
 *   <li><b>memory</b> - In-memory rate limiting using Bucket4j (single instance)</li>
 *   <li><b>redis</b> - Redis-backed rate limiting (distributed, multi-instance)</li>
 * </ul>
 *
 * <h4>Rate Limiting Parameters:</h4>
 * <ul>
 *   <li><b>requests-per-minute:</b> Base refill rate (tokens added per minute)</li>
 *   <li><b>burst-capacity:</b> Maximum tokens that can accumulate (allows bursts)</li>
 * </ul>
 *
 * <p><b>Example:</b> With 60 requests/min and 100 burst capacity:</p>
 * <ul>
 *   <li>Steady state: 1 request per second (60/min)</li>
 *   <li>Burst allowed: Up to 100 requests immediately if bucket is full</li>
 *   <li>Recovery: Bucket refills at 1 token/second</li>
 * </ul>
 *
 * <h4>Path Exclusions:</h4>
 * <p>Paths in {@code exclude-paths} bypass rate limiting entirely. Use for:</p>
 * <ul>
 *   <li>Health check endpoints ({@code /actuator/health})</li>
 *   <li>API documentation ({@code /swagger-ui/**})</li>
 *   <li>Public endpoints that don't require protection</li>
 * </ul>
 *
 * @author Steby Core Team
 * @since 1.0.0
 * @see RateLimitConfig
 */
@Component
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    /**
     * Whether rate limiting is enabled globally.
     * If false, all requests bypass rate limiting.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Rate limiter implementation provider.
     * Supported values: "memory", "redis"
     * Default: "memory"
     */
    private String provider = "memory";

    /**
     * Number of requests allowed per minute (refill rate).
     * This determines the steady-state rate at which tokens are added.
     * Default: 60 (1 request per second)
     */
    private int requestsPerMinute = 60;

    /**
     * Maximum number of tokens that can accumulate (burst capacity).
     * Allows clients to make bursts of requests if they have tokens saved up.
     * Should be greater than or equal to requestsPerMinute.
     * Default: 100
     */
    private int burstCapacity = 100;

    /**
     * List of path patterns to exclude from rate limiting.
     * Supports Ant-style patterns with wildcards:
     * - {@code *} matches any characters within a path segment
     * - {@code **} matches any number of path segments
     *
     * Examples:
     * - {@code /actuator/**} - all actuator endpoints
     * - {@code /api/public/*} - direct children of /api/public
     * - {@code /health} - exact match
     *
     * Default: empty list (no exclusions)
     */
    private List<String> excludePaths = new ArrayList<>();
}