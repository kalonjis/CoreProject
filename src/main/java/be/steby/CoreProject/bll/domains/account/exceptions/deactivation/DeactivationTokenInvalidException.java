package be.steby.CoreProject.bll.domains.account.exceptions.deactivation;

import be.steby.CoreProject.bll.common.exceptions.TokenValidityException;

public class DeactivationTokenInvalidException extends TokenValidityException {

    /**
     * Constructs a new {@code DeactivationTokenInvalidException} with the specified detail message.
     * The status code is set to 400 (Bad Request) by default.
     *
     * @param message the detail message
     */
    public DeactivationTokenInvalidException(String message) {
        super(message, 400);
    }

    /**
     * Constructs a new {@code DeactivationTokenInvalidException} with the specified detail message
     * and status code.
     *
     * @param message the detail message
     * @param status the HTTP status code
     */
    public DeactivationTokenInvalidException(String message, int status) {
        super(message, status);
    }
}
