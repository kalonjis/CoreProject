package be.steby.CoreProject.bll.exceptions;


public class OwnershipException extends CoreProjectException {
    /**
     * Constructs a new {@code OwnershipException} with the specified {@code detail message}.
     * The {@code status code} is set to {@code 403 (Unauthorized)} by default.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     */
    public OwnershipException(String message) {
        super(message, 403);
    }

    /**
     * Constructs a new {@code OwnershipException} with the specified {@code detail message} and {@code status code}.
     *
     * @param message the {@code detail message} (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param status  the {@code HTTP status code} (which is saved for later retrieval
     *                by the {@link #getStatus()} method).
     */
    public OwnershipException(String message, int status) {
        super(message, status);
    }
}
