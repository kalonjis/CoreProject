package be.steby.CoreProject.bll.common.exceptions.mail;

/**
 * Exception thrown when email delivery fails.
 * 
 * This exception is thrown by SmtpMailSender when:
 * - SMTP server is unreachable
 * - Circuit Breaker is OPEN (service temporarily unavailable)
 * - All retry attempts have been exhausted
 * - Authentication with mail server fails
 * 
 * HTTP Status: 503 (Service Unavailable) by default,
 * indicating a temporary failure that may resolve on retry.
 */
public class MailDeliveryException extends MailException {

    private static final int DEFAULT_STATUS = 503; // Service Unavailable

    /**
     * Creates a new mail delivery exception.
     *
     * @param message the detailed error message
     */
    public MailDeliveryException(String message) {
        super(message, DEFAULT_STATUS);
    }

    /**
     * Creates a new mail delivery exception with cause.
     *
     * @param message the detailed error message
     * @param cause   the underlying cause (e.g., MessagingException, MailException)
     */
    public MailDeliveryException(String message, Throwable cause) {
        super(message, DEFAULT_STATUS, cause);
    }

    /**
     * Creates a new mail delivery exception with custom status.
     *
     * @param message the detailed error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public MailDeliveryException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}