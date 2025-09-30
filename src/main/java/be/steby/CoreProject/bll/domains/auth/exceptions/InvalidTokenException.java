package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Generic exception for invalid token errors.
 * Base class for specific token validation exceptions.
 */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message, 400);
    }

    public InvalidTokenException() {
        super("Invalid or malformed token", 400);
    }
}