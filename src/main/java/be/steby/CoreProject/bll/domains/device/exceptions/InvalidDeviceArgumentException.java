package be.steby.CoreProject.bll.domains.device.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when invalid arguments are provided to device operations.
 * Equivalent to IllegalArgumentException but specific to device domain.
 */
public class InvalidDeviceArgumentException extends DeviceDomainException {

    public InvalidDeviceArgumentException(String message) {
        super(message, 400);
    }

    public InvalidDeviceArgumentException(String message, Throwable cause) {
        super(message, 400, cause);
    }
}