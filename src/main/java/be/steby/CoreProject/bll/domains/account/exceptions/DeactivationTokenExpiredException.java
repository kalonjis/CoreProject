package be.steby.CoreProject.bll.domains.account.exceptions;

import be.steby.CoreProject.bll.exceptions.TokenExpiredException;

public class DeactivationTokenExpiredException extends TokenExpiredException {

    /**
     * Constructs a new {@code DeactivationTokenExpiredException} with the specified detail message.
     * The status code is set to 410 (Gone) by default.
     *
     * @param message the detail message
     */
    public DeactivationTokenExpiredException(String message) {
        super(message, 410);
    }

    /**
     * Constructs a new {@code DeactivationTokenExpiredException} with the specified detail message
     * and status code.
     *
     * @param message the detail message
     * @param status the HTTP status code
     */
    public DeactivationTokenExpiredException(String message, int status) {
        super(message, status);
    }
}
