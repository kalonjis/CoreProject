package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to login with a blacklisted device.
 * This is a security-related exception that should result in a 403 Forbidden response.
 */
public class BlacklistedDeviceException extends DeviceSecurityException {

    public BlacklistedDeviceException(String message) {
        super(message, 403);
    }
}