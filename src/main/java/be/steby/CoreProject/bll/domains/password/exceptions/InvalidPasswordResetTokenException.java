package be.steby.CoreProject.bll.domains.password.exceptions;

/**
 * Exception thrown when password reset token validation fails.
 *
 * <p>This exception is thrown when JWT tokens used for password reset operations
 * are invalid, expired, malformed, or contain incorrect claims.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Permission token is expired or invalid</li>
 *   <li>SMS verification token has wrong purpose</li>
 *   <li>Missing required claims in token</li>
 *   <li>Token signature validation failed</li>
 *   <li>Token format is malformed</li>
 * </ul>
 *
 * <p>Results in HTTP 400 (Bad Request) responses by default.
 * Will be automatically caught and handled by the ControllerAdvisor.
 */
public class InvalidPasswordResetTokenException extends PasswordDomainException {

    /**
     * Creates a new exception with a descriptive message.
     * Uses HTTP 400 (Bad Request) as the default status code.
     *
     * @param message message describing the token validation failure
     */
    public InvalidPasswordResetTokenException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new exception with a message and custom status code.
     *
     * @param message message describing the token validation failure
     * @param status HTTP status code to return
     */
    public InvalidPasswordResetTokenException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new exception with a message and underlying cause.
     * Uses HTTP 400 (Bad Request) as the default status code.
     *
     * @param message message describing the token validation failure
     * @param cause the underlying cause of this validation failure
     */
    public InvalidPasswordResetTokenException(String message, Throwable cause) {
        super(message, 400);
        initCause(cause);
    }

    /**
     * Creates a new exception with a message, status code, and underlying cause.
     *
     * @param message message describing the token validation failure
     * @param status HTTP status code to return
     * @param cause the underlying cause of this validation failure
     */
    public InvalidPasswordResetTokenException(String message, int status, Throwable cause) {
        super(message, status);
        initCause(cause);
    }
}