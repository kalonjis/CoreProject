package be.steby.CoreProject.bll.exceptions;

/**
 * Exception thrown when a user or IP address exceeds the configured rate limit.
 *
 * <p>This exception extends {@link CoreProjectException} to integrate with the global
 * exception handling system via {@code ControllerAdvisor}. When thrown, it results
 * in an HTTP 429 (Too Many Requests) response.</p>
 *
 * <h4>HTTP Response:</h4>
 * <p>When caught by {@code ControllerAdvisor}, this exception produces:</p>
 * <ul>
 *   <li><b>Status Code:</b> 429 Too Many Requests</li>
 *   <li><b>Response Body:</b> JSON with error details</li>
 * </ul>
 *
 * @author Steby Core Team
 * @since 1.0.0
 * @see CoreProjectException
 * @see be.steby.CoreProject.pl.advisor.ControllerAdvisor
 * @see be.steby.CoreProject.il.filters.RateLimitFilter
 */
public class RateLimitExceededException extends CoreProjectException {

    /**
     * Creates a new rate limit exceeded exception with a message.
     * Default status is 429 (Too Many Requests).
     *
     * @param message the error message
     */
    public RateLimitExceededException(String message) {
        super(message, 429);
    }

    /**
     * Creates a new rate limit exceeded exception with a message and status code.
     *
     * @param message the error message
     * @param status  the HTTP status code
     */
    public RateLimitExceededException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new rate limit exceeded exception with a message and cause.
     * Default status is 429 (Too Many Requests).
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, 429, cause);
    }

    /**
     * Creates a new rate limit exceeded exception with message, status, and cause.
     *
     * @param message the error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public RateLimitExceededException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}