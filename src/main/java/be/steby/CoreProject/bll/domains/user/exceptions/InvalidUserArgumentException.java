package be.steby.CoreProject.bll.domains.user.exceptions;


/**
 * Exception thrown when invalid arguments are provided to user operations.
 * Equivalent to IllegalArgumentException but specific to user domain.
 */
public class InvalidUserArgumentException extends UserDomainException {

    public InvalidUserArgumentException(String message) {
        super(message, 400);
    }

    public InvalidUserArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}