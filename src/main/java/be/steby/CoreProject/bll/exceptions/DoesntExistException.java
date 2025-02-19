package be.steby.CoreProject.bll.exceptions;


/**
 * Custom exception class to indicate that a requested resource does not exist.
 * This exception extends {@code BiobanqueException}, allowing for consistent handling
 * of exceptions specific to the BioBanque application.
 */
public class DoesntExistException extends CoreProjectException {

    /**
     * Constructs a new {@code DoesntExistException} with specified detail message and automatically assign
     * the HTTP status code 404.
     *
     * @param message the detail message to explaining the cause of exception.
     */
    public DoesntExistException(String message) {
        super(message,404);
    }
}
