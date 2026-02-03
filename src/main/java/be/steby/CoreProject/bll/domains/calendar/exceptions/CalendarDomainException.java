package be.steby.CoreProject.bll.domains.calendar.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all calendar domain-related exceptions.
 * 
 * <p>This abstract class extends {@link CoreProjectException} to integrate
 * with the global exception handling system via {@code ControllerAdvisor}.</p>
 * 
 * <p>All specific calendar exceptions should extend this class to ensure
 * consistent error handling across the calendar domain.</p>
 * 
 * <h3>Common Use Cases</h3>
 * <ul>
 *   <li>Event not found errors</li>
 *   <li>Date validation failures</li>
 *   <li>Authorization/ownership violations</li>
 *   <li>Business rule violations</li>
 * </ul>
 * 
 * @see CoreProjectException
 * @see CalendarEventNotFoundException
 * @author Steby Corp
 */
public abstract class CalendarDomainException extends CoreProjectException {

    /**
     * Creates a new calendar domain exception with a message.
     * Default status is 400 (Bad Request).
     * 
     * @param message the error message
     */
    public CalendarDomainException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new calendar domain exception with a message and status code.
     * 
     * @param message the error message
     * @param status the HTTP status code
     */
    public CalendarDomainException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new calendar domain exception with a message and cause.
     * Default status is 400 (Bad Request).
     * 
     * @param message the error message
     * @param cause the underlying cause
     */
    public CalendarDomainException(String message, Throwable cause) {
        super(message, 400, cause);
    }

    /**
     * Creates a new calendar domain exception with message, status, and cause.
     * 
     * @param message the error message
     * @param status the HTTP status code
     * @param cause the underlying cause
     */
    public CalendarDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}