package be.steby.CoreProject.bll.common.exceptions.mail;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

/**
 * Base exception for all mail/notification related errors.
 * 
 * This serves as the parent class for all mail domain exceptions,
 * providing a clear exception hierarchy for email operations.
 * 
 * Exception hierarchy:
 * - MailException (this class)
 *   ├── MailDeliveryException (SMTP failures, circuit breaker open)
 *   ├── MailPreparationException (template errors, invalid recipients)
 *   └── MailServiceUnavailableException (service temporarily down)
 * 
 * @see CoreProjectException
 */
public class MailException extends CoreProjectException {

    /**
     * Creates a new mail exception with default 500 status.
     *
     * @param message the detailed error message
     */
    public MailException(String message) {
        super(message, 500);
    }

    /**
     * Creates a new mail exception with custom status.
     *
     * @param message the detailed error message
     * @param status  the HTTP status code
     */
    public MailException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new mail exception with message and cause.
     *
     * @param message the detailed error message
     * @param cause   the underlying cause
     */
    public MailException(String message, Throwable cause) {
        super(message, 500, cause);
    }

    /**
     * Creates a new mail exception with message, status, and cause.
     *
     * @param message the detailed error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public MailException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}