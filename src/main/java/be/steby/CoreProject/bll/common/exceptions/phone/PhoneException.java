package be.steby.CoreProject.bll.common.exceptions.phone;


import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all phone operations.
 * Extends CoreProjectException to follow the project's exception hierarchy.
 */
public class PhoneException extends CoreProjectException {

    public PhoneException(String message) {
        super(message);
    }

    public PhoneException(String message, int status) {
        super(message, status);
    }

    public PhoneException(String message, Throwable cause) {
        super(message, cause);
    }
}