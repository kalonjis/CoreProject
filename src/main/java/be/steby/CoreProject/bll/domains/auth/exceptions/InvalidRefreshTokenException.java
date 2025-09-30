package be.steby.CoreProject.bll.domains.auth.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends CoreProjectException {

    public InvalidRefreshTokenException(String message) {
        super(message, 400);
    }

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token", 400);
    }
}