package be.steby.CoreProject.bll.common.exceptions;

import lombok.Getter;

/**
 * Exception thrown when the device trust level is insufficient to access a resource.
 */
@Getter
public class DeviceTrustLevelException extends CoreProjectException {

    /**
     * Constructs a new {@link DeviceTrustLevelException} with the specified detail message.
     * The status code is set to {@code 403 (Forbidden)} by default.
     *
     * @param message       the detail message (which is saved for later retrieval
     *                      by the {@link #getMessage()} method).
     */
    public DeviceTrustLevelException(String message) {
        super(message, 403); // 403 Forbidden

    }

    /**
     * Constructs a new {@link DeviceTrustLevelException} with the specified detail message and status code.
     *
     * @param message       the detail message (which is saved for later retrieval
     *                      by the {@link #getMessage()} method).
     * @param status        the HTTP status code (which is saved for later retrieval
     *                      by the {@link #getStatus()} method).
     */
    public DeviceTrustLevelException(String message, int status) {
        super(message, status);
    }
}