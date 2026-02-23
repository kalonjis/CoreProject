package be.steby.CoreProject.bll.domains.account.exceptions.deactivation;

import be.steby.CoreProject.bll.common.exceptions.TokenExpiredException;

public class DeactivationTokenExpiredException extends TokenExpiredException {

    /**
     * Constructs a new TokenExpiredException with the specified detail message.
     * The status code is set to 498 by default.
     */
    public DeactivationTokenExpiredException(String message) {
        super(message, 498);
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
