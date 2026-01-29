package be.steby.CoreProject.bll.domains.sport.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all sport domain-related exceptions.
 *
 * <p>This abstract class extends {@link CoreProjectException} to integrate
 * with the global exception handling system via {@code ControllerAdvisor}.</p>
 *
 * <p>All specific sport exceptions should extend this class to ensure
 * consistent error handling across the sport domain.</p>
 *
 * @see CoreProjectException
 * @see GpxParsingException
 * @see SportTrackNotFoundException
 * @see GpxAlreadyImportedException
 */
public abstract class SportDomainException extends CoreProjectException {

    /**
     * Creates a new sport domain exception with a message.
     * Default status is 400 (Bad Request).
     *
     * @param message the error message
     */
    public SportDomainException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new sport domain exception with a message and status code.
     *
     * @param message the error message
     * @param status  the HTTP status code
     */
    public SportDomainException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new sport domain exception with a message and cause.
     * Default status is 400 (Bad Request).
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public SportDomainException(String message, Throwable cause) {
        super(message, 400, cause);
    }

    /**
     * Creates a new sport domain exception with message, status, and cause.
     *
     * @param message the error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public SportDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}