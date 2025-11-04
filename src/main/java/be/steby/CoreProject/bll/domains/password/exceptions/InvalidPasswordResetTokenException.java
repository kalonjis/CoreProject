package be.steby.CoreProject.bll.domains.password.exceptions;

/**
 * Exception thrown when password reset token validation fails.
 * 
 * <p>This exception is specifically for password reset SMS token validation
 * failures, similar to InvalidPhoneVerificationTokenException but for the
 * password reset domain.
 * 
 * <p>Common scenarios:
 * <ul>
 *   <li>Token is expired or invalid</li>
 *   <li>Token has wrong purpose (not PASSWORD_RESET_SMS)</li>
 *   <li>Missing required claims in token</li>
 *   <li>Token signature validation failed</li>
 * </ul>
 * 
 * <p>Results in HTTP 400 (Bad Request) responses by default.
 */
public class InvalidPasswordResetTokenException extends PasswordDomainException {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param message message describing the token validation failure
     */
    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and underlying cause.
     *
     * @param message message describing the token validation failure
     * @param cause the underlying cause of this validation failure
     */
    public InvalidPasswordResetTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}