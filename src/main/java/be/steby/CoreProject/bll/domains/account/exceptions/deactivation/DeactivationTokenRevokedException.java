package be.steby.CoreProject.bll.domains.account.exceptions.deactivation;

import be.steby.CoreProject.bll.exceptions.TokenRevokedException;

public class DeactivationTokenRevokedException extends TokenRevokedException {

    /**
     * Constructs a new {@code DeactivationTokenRevokedException} with the specified detail message.
     * The status code is set to 410 (Gone) by default.
     *
     * @param message the detail message
     */
    public DeactivationTokenRevokedException(String message) {
        super(message, 410);
    }

    /**
     * Constructs a new {@code DeactivationTokenRevokedException} with the specified detail message
     * and status code.
     *
     * @param message the detail message
     * @param status the HTTP status code
     */
    public DeactivationTokenRevokedException(String message, int status) {
        super(message, status);
    }
}
