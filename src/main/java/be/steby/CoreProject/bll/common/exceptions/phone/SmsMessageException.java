package be.steby.CoreProject.bll.common.exceptions.phone;

/**
 * Exception thrown when SMS message content is invalid.
 * 
 * This exception is used when:
 * - Message is null or empty
 * - Message content violates SMS constraints
 * - Message format is invalid
 */
public class SmsMessageException extends PhoneException {

    public SmsMessageException(String message) {
        super(message, 400); // Bad Request - client error
    }

    public SmsMessageException(String message, int status) {
        super(message, status);
    }

    public SmsMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}