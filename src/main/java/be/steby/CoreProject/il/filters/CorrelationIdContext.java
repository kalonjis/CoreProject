package be.steby.CoreProject.il.filters;

import org.slf4j.MDC;

import java.util.Optional;

/**
 * Utility class providing static access to the current request's Correlation ID.
 *
 * <p>This class abstracts the underlying MDC (Mapped Diagnostic Context) mechanism,
 * providing a clean API for accessing and managing correlation IDs throughout
 * the application.</p>
 *
 * <h4>Purpose:</h4>
 * <ul>
 *   <li><strong>Abstraction:</strong> Decouples application code from SLF4J MDC internals</li>
 *   <li><strong>Convenience:</strong> Simple static methods instead of MDC key management</li>
 *   <li><strong>Flexibility:</strong> Easy to change underlying mechanism if needed</li>
 *   <li><strong>Type safety:</strong> Provides Optional-based access for null safety</li>
 * </ul>
 *
 * <h4>Usage Examples:</h4>
 * <pre>{@code
 * // Get current correlation ID (optional)
 * Optional<String> correlationId = CorrelationIdContext.get();
 *
 * // Get with default value
 * String id = CorrelationIdContext.getOrDefault("no-correlation-id");
 *
 * // Store in ActivityLog entity
 * activityLog.setCorrelationId(CorrelationIdContext.getOrDefault("unknown"));
 *
 * // Check if present
 * if (CorrelationIdContext.isPresent()) {
 *     // ... use the ID
 * }
 * }</pre>
 *
 * <h4>Thread Safety:</h4>
 * <p>This class is thread-safe as it delegates to MDC, which uses ThreadLocal storage.
 * Each thread has its own isolated correlation ID value.</p>
 *
 * <h4>Async Considerations:</h4>
 * <p>When using async operations (@Async), the MDC context is not automatically
 * propagated to child threads. Use {@link be.steby.CoreProject.il.configs.MdcTaskDecorator}
 * to ensure correlation IDs are preserved across async boundaries.</p>
 *
 * @see CorrelationIdFilter
 * @see be.steby.CoreProject.il.configs.MdcTaskDecorator
 * @see org.slf4j.MDC
 */
public final class CorrelationIdContext {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private CorrelationIdContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Retrieves the current correlation ID as an Optional.
     *
     * <p>This method is the preferred way to access the correlation ID when
     * the absence of an ID needs to be handled explicitly.</p>
     *
     * @return Optional containing the correlation ID, or empty if not set
     */
    public static Optional<String> get() {
        return Optional.ofNullable(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    /**
     * Retrieves the current correlation ID or a default value.
     *
     * <p>Useful when a non-null value is required, such as when storing
     * in database entities or including in error responses.</p>
     *
     * @param defaultValue the value to return if no correlation ID is set
     * @return the correlation ID or the default value
     */
    public static String getOrDefault(String defaultValue) {
        return get().orElse(defaultValue);
    }

    /**
     * Retrieves the current correlation ID or "N/A".
     *
     * <p>Convenience method for logging or display purposes where
     * a placeholder is acceptable.</p>
     *
     * @return the correlation ID or "N/A" if not set
     */
    public static String getOrNA() {
        return getOrDefault("N/A");
    }

    /**
     * Checks if a correlation ID is currently set.
     *
     * @return true if a correlation ID exists in the current context
     */
    public static boolean isPresent() {
        return get().isPresent();
    }

    /**
     * Sets the correlation ID in the current thread context.
     *
     * <p><strong>Warning:</strong> This method should rarely be used directly.
     * The {@link CorrelationIdFilter} automatically manages the correlation ID
     * for HTTP requests. This method is primarily intended for:</p>
     * <ul>
     *   <li>Scheduled tasks that run outside HTTP context</li>
     *   <li>Message queue consumers</li>
     *   <li>Test scenarios</li>
     * </ul>
     *
     * @param correlationId the correlation ID to set
     * @throws IllegalArgumentException if correlationId is null or blank
     */
    public static void set(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or blank");
        }
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);
    }

    /**
     * Clears the correlation ID from the current thread context.
     *
     * <p><strong>Warning:</strong> This method should rarely be used directly.
     * The {@link CorrelationIdFilter} automatically clears the correlation ID
     * after request completion. This method is primarily intended for:</p>
     * <ul>
     *   <li>Cleanup in scheduled tasks</li>
     *   <li>Test teardown</li>
     *   <li>Manual thread pool management</li>
     * </ul>
     */
    public static void clear() {
        MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
    }

    /**
     * Returns the MDC key used for storing the correlation ID.
     *
     * <p>Useful when configuring logging patterns or integrating with
     * external systems that need to know the MDC key name.</p>
     *
     * @return the MDC key for correlation ID
     */
    public static String getMdcKey() {
        return CorrelationIdFilter.CORRELATION_ID_MDC_KEY;
    }
}