package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when user provides invalid credentials (username/password).
 * This should result in a 401 Unauthorized response.
 */
public class InvalidCredentialsException extends AuthDomainException {

    public InvalidCredentialsException(String message) {
        super(message, 401);
    }

}
