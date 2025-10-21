package be.steby.CoreProject.bll.common.exceptions.phone;


/**
 * Exception thrown when phone verification token is invalid or expired.
 * Used when JWT phone verification token validation fails.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class InvalidPhoneVerificationTokenException extends PhoneException {
    
    public InvalidPhoneVerificationTokenException(String message) {
        super(message);
    }

    public InvalidPhoneVerificationTokenException(String message, int status) {
        super(message, status);
    }
    
    public InvalidPhoneVerificationTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}