package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to login with a blacklisted device.
 * This is a security-related exception that should result in a 403 Forbidden response.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class BlacklistedDeviceException extends AuthenticationException {

    public BlacklistedDeviceException(String message) {
        super(message);
    }

    public BlacklistedDeviceException(String message, int status) {
        super(message, status);
    }
}