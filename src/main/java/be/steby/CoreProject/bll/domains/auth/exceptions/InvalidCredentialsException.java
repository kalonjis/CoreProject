package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when user provides invalid credentials (username/password).
 * This should result in a 401 Unauthorized response.
 */
public class InvalidCredentialsException extends AuthenticationException {

    public InvalidCredentialsException(String message) {
        super(message, 401);
    }

}
