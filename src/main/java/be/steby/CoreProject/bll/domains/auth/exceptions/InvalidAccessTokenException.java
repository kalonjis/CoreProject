package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when an access token is invalid, malformed, or missing.
 * Used during token generation and validation in the authentication flow.
 */
public class InvalidAccessTokenException extends InvalidTokenException {

    public InvalidAccessTokenException(String message) {
        super(message);
    }

    public InvalidAccessTokenException() {
        super("Invalid or missing access token");
    }
}