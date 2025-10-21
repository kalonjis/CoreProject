package be.steby.CoreProject.bll.common.exceptions.phone;

/**
 * Exception thrown when SMS sending operation fails.
 * Similar to EmailSendingException but for SMS operations.
 */
public class SmsSendingException extends PhoneException {

    public SmsSendingException(String message) {
        super(message);
    }

    public SmsSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}