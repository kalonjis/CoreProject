package be.steby.CoreProject.bll.domains.auth.exceptions;

public class InvalidRefreshTokenException extends AuthenticationException {

    public InvalidRefreshTokenException(String message) {
        super(message, 400);
    }

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token", 400);
    }
}