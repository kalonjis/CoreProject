package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General exception for device security-related issues during authentication.
 * This serves as a base class for more specific device security exceptions.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class DeviceSecurityException extends AuthenticationException {

    public DeviceSecurityException(String message) {
        super(message);
    }

    public DeviceSecurityException(String message, int status) {
        super(message, status);
    }
}
