package be.steby.CoreProject.bll.domains.auth.exceptions;

public class InvalidRefreshTokenException extends InvalidTokenException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}