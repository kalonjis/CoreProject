package be.steby.CoreProject.bll.domains.user.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all user domain-related errors.
 * This serves as the parent class for all user domain exceptions,
 * providing a clear exception hierarchy.
 *
 */
public class UserDomainException extends CoreProjectException {

    /**
     * Creates a new user domain exception with default 400 status.
     *
     * @param message The detailed error message
     */
    public UserDomainException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new user domain exception with custom status.
     *
     * @param message The detailed error message
     * @param status The HTTP status code
     */
    public UserDomainException(String message, int status) {
        super(message, status);
    }


    public UserDomainException(String message, Throwable cause) {
        super(message, cause);
    }

}