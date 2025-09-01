package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user account has been disabled by an administrator.
 * This should result in a 403 Forbidden response.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountDisabledException extends AuthenticationException {

    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException(String message, int status) {
        super(message, status);
    }
}