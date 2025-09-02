package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;

/**
 * Exception thrown when a user account or IP address is temporarily locked
 * due to excessive failed login attempts.
 * This is a temporary security measure to prevent brute force attacks.
 */
public class AccountTemporarilyLockedException extends AuthenticationException {

    public AccountTemporarilyLockedException(String message) {
        super(message, 423);
    }
}