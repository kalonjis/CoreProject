
package be.steby.CoreProject.bll.common.exceptions;

/**
 * Exception thrown when token security validation fails.
 *
 * This exception is specifically designed for token-related security issues such as:
 * - Invalid token format (expecting secured/encrypted vs plain token)
 * - Token decryption failures
 * - Token format validation errors
 * - Security policy violations related to token handling
 *
 * Extends {@link CoreProjectException} to be automatically handled by the global
 * exception handler ({@code ControllerAdvisor}).
 *

 * @see CoreProjectException
 * @see be.steby.CoreProject.bll.common.services.tokens.BaseTokenService
 */
public class TokenSecurityException extends CoreProjectException {

    /**
     * Constructs a new TokenSecurityException with the specified detail message.
     * The HTTP status code is set to 400 (Bad Request) by default.
     *
     * @param message the detail message explaining the security violation
     */
    public TokenSecurityException(String message) {
        super(message, 400);
    }

    /**
     * Constructs a new TokenSecurityException with the specified detail message
     * and HTTP status code.
     *
     * @param message the detail message explaining the security violation
     * @param status the HTTP status code to return to the client
     */
    public TokenSecurityException(String message, int status) {
        super(message, status);
    }

    /**
     * Constructs a new TokenSecurityException with the specified detail message
     * and the underlying cause that triggered this security exception.
     * The HTTP status code is set to 400 (Bad Request) by default.
     *
     * <p>This constructor is particularly useful when wrapping lower-level exceptions
     * (such as cryptographic exceptions) while preserving the full stack trace
     * for debugging purposes.</p>
     *
     * @param message the detail message explaining the security violation
     * @param cause the underlying exception that caused this security violation
     */
    public TokenSecurityException(String message, Throwable cause) {
        super(message, 400);
        initCause(cause);
    }

    /**
     * Constructs a new TokenSecurityException with the specified detail message,
     * HTTP status code, and the underlying cause.
     *
     * <p>This is the most complete constructor, providing full control over both
     * the HTTP response status and exception chaining.</p>
     *
     * @param message the detail message explaining the security violation
     * @param status the HTTP status code to return to the client
     * @param cause the underlying exception that caused this security violation
     */
    public TokenSecurityException(String message, int status, Throwable cause) {
        super(message, status);
        initCause(cause);
    }
}
