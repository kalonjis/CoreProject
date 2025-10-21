package be.steby.CoreProject.bll.common.exceptions.phone;

/**
 * Exception thrown when a phone number format is invalid or cannot be parsed.
 * 
 * This exception is used when:
 * - Phone number is null or empty
 * - Phone number format is not recognized (not Belgian mobile or international)
 * - Phone number contains invalid characters that cannot be cleaned
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class InvalidPhoneNumberFormatException extends PhoneException {

    public InvalidPhoneNumberFormatException(String message) {
        super(message);
    }

    public InvalidPhoneNumberFormatException(String message, int status) {
        super(message, status);
    }

    public InvalidPhoneNumberFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}