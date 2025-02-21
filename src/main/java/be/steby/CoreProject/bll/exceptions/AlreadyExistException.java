package be.steby.CoreProject.bll.exceptions;


/**
 * Custom exception class to indicate that a requested resource does already exist.
 * This exception extends {@code BiobanqueException}, allowing for consistent handling
 * of exceptions specific to the BioBanque application.
 */
public class AlreadyExistException extends CoreProjectException {


    /**
     * Constructs a new {@code AlreadyExistException} with specified detail message and automatically assign
     * the HTTP status code 409.
     *
     * @param message the detail message to explaining the cause of exception.
     */
    public AlreadyExistException(String message) {
        super(message,409);
    }
}
