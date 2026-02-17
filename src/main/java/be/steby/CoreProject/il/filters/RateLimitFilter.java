package be.steby.CoreProject.il.filters;

import be.steby.CoreProject.bll.exceptions.RateLimitExceededException;
import be.steby.CoreProject.il.ratelimit.RateLimiter;
import be.steby.CoreProject.il.ratelimit.config.RateLimitProperties;
import be.steby.CoreProject.il.ratelimit.models.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;

import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.PUBLIC_ROUTES;

/**
 * HTTP filter that enforces global API rate limiting per user or IP address.
 *
 * <p>This filter intercepts incoming requests and checks if the user/IP has exceeded
 * their rate limit. If the limit is exceeded, the request is rejected with HTTP 429.
 * The filter runs after authentication (JwtFilter) to allow rate limiting by authenticated
 * user when possible.</p>
 *
 * <h4>Filter Order:</h4>
 * <pre>
 * 1. CorrelationIdFilter (HIGHEST_PRECEDENCE)
 * 2. JwtFilter (Order 1) - Authentication
 * 3. MustChangePasswordFilter (Order 2) - Password enforcement
 * 4. RateLimitFilter (Order 3) - THIS FILTER
 * 5. Spring Security Filters
 * 6. Controller
 * </pre>
 *
 * <h4>Rate Limit Key Resolution:</h4>
 * <p>The filter determines the rate limit key using this priority:</p>
 * <ol>
 *   <li><b>Authenticated User:</b> "user:{username}" - if user is authenticated</li>
 *   <li><b>IP Address:</b> "ip:{ip_address}" - for anonymous users</li>
 * </ol>
 *
 * <p>This approach ensures authenticated users are rate limited individually,
 * while anonymous users share rate limits by IP address.</p>
 *
 * <h4>Exclusions:</h4>
 * <p>The following requests bypass rate limiting:</p>
 * <ul>
 *   <li><b>Public routes:</b> All routes in {@link SecurityRoutesAggregator#PUBLIC_ROUTES}</li>
 *   <li><b>Excluded paths:</b> Paths configured in {@code rate-limit.exclude-paths}</li>
 * </ul>
 *
 * <h4>Response Headers:</h4>
 * <p>For allowed requests, the filter adds informational headers:</p>
 * <ul>
 *   <li>{@code X-RateLimit-Limit} - Maximum requests allowed</li>
 *   <li>{@code X-RateLimit-Remaining} - Tokens remaining</li>
 *   <li>{@code X-RateLimit-Reset} - Unix timestamp when limit resets</li>
 * </ul>
 *
 * <h4>Exception Handling:</h4>
 * <p>When rate limit is exceeded, the filter throws {@link RateLimitExceededException}
 * which is delegated to {@link be.steby.CoreProject.pl.advisor.ControllerAdvisor}
 * for consistent error response formatting.</p>
 *
 * <h4>Configuration:</h4>
 * <p>This filter is only active when {@code rate-limit.enabled=true} in application properties.
 * Set to {@code false} to completely disable rate limiting.</p>
 *
 * @author Steby Core Team
 * @since 1.0.0
 * @see RateLimiter
 * @see RateLimitProperties
 * @see RateLimitExceededException
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final HandlerExceptionResolver exceptionResolver;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Constructs the rate limit filter with required dependencies.
     *
     * @param rateLimiter the rate limiter implementation (in-memory or Redis)
     * @param properties rate limiting configuration properties
     * @param exceptionResolver Spring's exception resolver for delegating to ControllerAdvisor
     */
    public RateLimitFilter(
            RateLimiter rateLimiter,
            RateLimitProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (shouldSkipRateLimiting(requestURI)) {
            log.trace("Skipping rate limiting for excluded path: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveRateLimitKey(request);
        RateLimitResult result = rateLimiter.tryConsume(key);

        if (result.isAllowed()) {
            addRateLimitHeaders(response, result);
            log.trace("Rate limit check passed for key: {} (remaining: {})", 
                    key, result.tokensRemaining());
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {} (retry after: {}s)", 
                    key, result.retryAfterSeconds());
            
            RateLimitExceededException exception = new RateLimitExceededException(
                    "Too many requests. Please try again later."
            );
            
            exceptionResolver.resolveException(request, response, null, exception);
        }
    }

    /**
     * Determines if rate limiting should be skipped for this request.
     *
     * <p>Skips rate limiting for:</p>
     * <ul>
     *   <li>Public routes (no authentication required)</li>
     *   <li>Explicitly excluded paths from configuration</li>
     * </ul>
     *
     * @param requestURI the request URI
     * @return true if rate limiting should be skipped
     */
    private boolean shouldSkipRateLimiting(String requestURI) {
        return isPublicRoute(requestURI) || isExcludedPath(requestURI);
    }

    /**
     * Checks if the request URI matches any public route.
     *
     * <p>Public routes don't require authentication, so rate limiting by user
     * is not applicable. These routes may still be rate limited by IP if needed.</p>
     *
     * @param requestURI the request URI
     * @return true if the URI is a public route
     */
    private boolean isPublicRoute(String requestURI) {
        return Arrays.stream(PUBLIC_ROUTES)
                .anyMatch(publicRoute -> {
                    if (publicRoute.endsWith("/**")) {
                        String prefix = publicRoute.substring(0, publicRoute.length() - 3);
                        return requestURI.startsWith(prefix);
                    }
                    if (publicRoute.endsWith("/*")) {
                        String prefix = publicRoute.substring(0, publicRoute.length() - 2);
                        return requestURI.startsWith(prefix);
                    }
                    return requestURI.equals(publicRoute);
                });
    }

    /**
     * Checks if the request URI matches any excluded path pattern.
     *
     * <p>Uses Ant-style path matching to support wildcards:</p>
     * <ul>
     *   <li>{@code /actuator/**} - all actuator endpoints</li>
     *   <li>{@code /swagger-ui/*} - direct children of swagger-ui</li>
     * </ul>
     *
     * @param requestURI the request URI
     * @return true if the URI matches an excluded pattern
     */
    private boolean isExcludedPath(String requestURI) {
        return properties.getExcludePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }

    /**
     * Resolves the rate limit key for the current request.
     *
     * <p>Priority order:</p>
     * <ol>
     *   <li>Authenticated user: "user:{username}"</li>
     *   <li>Anonymous/unauthenticated: "ip:{ip_address}"</li>
     * </ol>
     *
     * @param request the HTTP request
     * @return rate limit key (never null)
     */
    private String resolveRateLimitKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() 
                && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            log.trace("Rate limiting by authenticated user: {}", username);
            return "user:" + username;
        }

        String ipAddress = getClientIP(request);
        log.trace("Rate limiting by IP address: {}", ipAddress);
        return "ip:" + ipAddress;
    }

    /**
     * Extracts the client's IP address from the request.
     *
     * <p>Checks {@code X-Forwarded-For} header first (for proxied requests),
     * then falls back to {@code RemoteAddr}.</p>
     *
     * @param request the HTTP request
     * @return client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Adds rate limit information to response headers.
     *
     * <p>These headers inform clients about their current rate limit status
     * and follow standard conventions used by major APIs (GitHub, Twitter, etc.).</p>
     *
     * @param response the HTTP response
     * @param result the rate limit result
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.tokensRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetAt().getEpochSecond()));
    }
}