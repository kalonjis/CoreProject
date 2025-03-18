package be.steby.CoreProject.bll.exceptions;



public class UserAuthenticationStateException extends CoreProjectException {

    /**
     * Constructs a new {@code UserAuthenticationStateException} with specified detail message and automatically assign
     * the HTTP status code 401.
     *
     * @param message the detail message to explaining the cause of exception.
     */
    public UserAuthenticationStateException(String message) {
        super(message,400);
    }


    /**
     * Constructs a new {@code AuthenticationException} with the specified {@code detail message} and {@code status code}.
     *
     * @param message the {@code detail message} (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param status  the {@code HTTP status code} (which is saved for later retrieval
     *                by the {@link #getStatus()} method).
     */
    public UserAuthenticationStateException(String message, int status) {
        super(message, status);
    }
}
