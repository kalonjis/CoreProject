package be.steby.CoreProject.bll.domains.password.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

/**
 * Base exception for all password domain-related exceptions.
 *
 * <p>This class extends CoreProjectException to integrate with the global error handling system.
 * All password-related business logic exceptions should extend this class to provide
 * consistent error handling and HTTP status code mapping.
 *
 * <p>Common use cases:
 * <ul>
 *   <li>Password validation failures</li>
 *   <li>Password reset token issues</li>
 *   <li>Password change authorization problems</li>
 *   <li>Password policy violations</li>
 * </ul>
 */
public abstract class PasswordDomainException extends CoreProjectException {

    /**
     * Creates a new exception with a message and default status code 400 (Bad Request).
     *
     * @param message message describing the error
     */
    public PasswordDomainException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new exception with a message and specific status code.
     *
     * @param message message describing the error
     * @param status HTTP status code
     */
    public PasswordDomainException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new exception with a message and underlying cause.
     *
     * @param message message describing the error
     * @param cause the underlying cause of this exception
     */
    public PasswordDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}