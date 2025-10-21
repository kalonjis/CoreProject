package be.steby.CoreProject.bll.domains.auth.exceptions.phone;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when a user doesn't have a valid phone number configured
 * for SMS-based operations like 2FA.
 * 
 * This exception extends AuthenticationException as it's related to authentication
 * flow interruption due to missing or invalid phone configuration.
 */
public class InvalidPhoneNumberException extends AuthenticationException {

    public InvalidPhoneNumberException(String message) {
        super(message);
    }

    public InvalidPhoneNumberException(String message, int status) {
        super(message, status);
    }


    public InvalidPhoneNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}