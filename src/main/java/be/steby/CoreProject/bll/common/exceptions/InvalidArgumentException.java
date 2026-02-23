package be.steby.CoreProject.bll.common.exceptions;

/**
 * Exception thrown when invalid arguments are provided to service operations.
 * 
 * <p>This is the common equivalent to IllegalArgumentException but integrated
 * with the project's exception hierarchy. It provides consistent error handling
 * for argument validation across all domains.
 * 
 * <p>This exception should be used for:
 * <ul>
 *   <li>Invalid parameter values (null, empty, out of range)</li>
 *   <li>Invalid method arguments that fail business validation</li>
 *   <li>Configuration parameter validation failures</li>
 *   <li>Any argument that doesn't meet expected criteria</li>
 * </ul>
 * 
 * <p>Results in HTTP 400 (Bad Request) responses by default.
 */
public class InvalidArgumentException extends CoreProjectException {

    /**
     * Creates a new exception with a descriptive message.
     * Uses HTTP 400 (Bad Request) as the default status code.
     *
     * @param message message describing the invalid argument
     */
    public InvalidArgumentException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new exception with a message and custom status code.
     *
     * @param message message describing the invalid argument
     * @param status HTTP status code
     */
    public InvalidArgumentException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new exception with a message and underlying cause.
     *
     * @param message message describing the invalid argument
     * @param cause the underlying cause of this exception
     */
    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with a message, status code, and underlying cause.
     *
     * @param message message describing the invalid argument
     * @param status HTTP status code
     * @param cause the underlying cause of this exception
     */
    public InvalidArgumentException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}