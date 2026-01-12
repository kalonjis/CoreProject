package be.steby.CoreProject.il.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP filter that manages Correlation IDs for request tracing across the application.
 *
 * <p>This filter intercepts every incoming HTTP request and ensures a unique correlation ID
 * is available throughout the request lifecycle. The ID is propagated via SLF4J's MDC
 * (Mapped Diagnostic Context), making it automatically available in all log statements.</p>
 *
 * <h4>Correlation ID Flow:</h4>
 * <ol>
 *   <li>Check if client provided a correlation ID via {@code X-Correlation-ID} header</li>
 *   <li>If not provided, generate a new UUID</li>
 *   <li>Store the ID in MDC for logging</li>
 *   <li>Add the ID to the response header for client reference</li>
 *   <li>Clean up MDC after request completion to prevent memory leaks</li>
 * </ol>
 *
 * <h4>Usage Benefits:</h4>
 * <ul>
 *   <li><strong>Debugging:</strong> Filter logs by correlation ID to trace a single request</li>
 *   <li><strong>Support:</strong> Ask users for the correlation ID from error responses</li>
 *   <li><strong>Distributed tracing:</strong> Pass the ID to downstream services</li>
 *   <li><strong>Async tracking:</strong> Combined with MdcTaskDecorator, traces async operations</li>
 * </ul>
 *
 * <h4>Header Names:</h4>
 * <ul>
 *   <li>Request header: {@code X-Correlation-ID} (optional, client-provided)</li>
 *   <li>Response header: {@code X-Correlation-ID} (always present)</li>
 *   <li>MDC key: {@code correlationId}</li>
 * </ul>
 *
 * <h4>Filter Order:</h4>
 * <p>This filter runs with {@link Ordered#HIGHEST_PRECEDENCE} to ensure the correlation ID
 * is available before any other filter (including security filters) executes. This guarantees
 * that even authentication failures are properly traced.</p>
 *
 * <h4>Thread Safety:</h4>
 * <p>MDC is thread-local, so each request thread has its own isolated correlation ID.
 * For async operations, see {@link be.steby.CoreProject.il.configs.MdcTaskDecorator}.</p>
 *
 * @see org.slf4j.MDC
 * @see be.steby.CoreProject.il.configs.MdcTaskDecorator
 * @see CorrelationIdContext
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * HTTP header name for correlation ID.
     * Used for both request (optional) and response (always set).
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * MDC key under which the correlation ID is stored.
     * Referenced in logback configuration pattern.
     */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /**
     * Processes each HTTP request to manage correlation ID lifecycle.
     *
     * <p>This method is guaranteed to be called only once per request, even in
     * dispatch scenarios (forwards, includes, async dispatches).</p>
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = null;

        try {
            // 1. Extract or generate correlation ID
            correlationId = extractOrGenerateCorrelationId(request);

            // 2. Store in MDC for automatic inclusion in logs
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // 3. Add to response header for client reference
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // 4. Log request start (debug level to avoid noise)
            log.debug("Request started: {} {} - CorrelationId: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    correlationId);

            // 5. Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // 6. Log request completion
            if (correlationId != null) {
                log.debug("Request completed: {} {} - CorrelationId: {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        correlationId);
            }

            // 7. CRITICAL: Clean up MDC to prevent memory leaks and cross-request contamination
            // This is essential because servlet containers reuse threads
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Extracts correlation ID from request header or generates a new one.
     *
     * <p>If the client provides a correlation ID via the {@code X-Correlation-ID} header,
     * it will be reused. This is useful when:</p>
     * <ul>
     *   <li>An API gateway has already assigned an ID</li>
     *   <li>A frontend wants to correlate its logs with backend logs</li>
     *   <li>Microservices need to maintain trace continuity</li>
     * </ul>
     *
     * <p>If no header is provided, a new UUID is generated.</p>
     *
     * @param request the HTTP request to extract the header from
     * @return the correlation ID (either from header or newly generated)
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateCorrelationId();
            log.trace("Generated new correlation ID: {}", correlationId);
        } else {
            log.trace("Using client-provided correlation ID: {}", correlationId);
        }

        return correlationId;
    }

    /**
     * Generates a new unique correlation ID.
     *
     * <p>Uses UUID for guaranteed uniqueness. The full UUID is used rather than
     * a shortened version to ensure no collisions occur in high-traffic scenarios.</p>
     *
     * @return a new UUID string
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}