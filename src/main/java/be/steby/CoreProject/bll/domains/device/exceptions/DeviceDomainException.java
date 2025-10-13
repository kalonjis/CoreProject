package be.steby.CoreProject.bll.domains.device.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all device domain exceptions.
 * Extends CoreProjectException to maintain consistency across the application.
 */
public class DeviceDomainException extends CoreProjectException {

    public DeviceDomainException(String message) {
        super(message);
    }

    public DeviceDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceDomainException(String message, int httpStatus) {
        super(message, httpStatus);
    }

    public DeviceDomainException(String message, int httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }
}