package be.steby.CoreProject.bll.domains.calendar.exceptions;

/**
 * Exception thrown when a calendar event is not found by its public ID.
 * 
 * <p>Extends {@link CalendarDomainException} to integrate with the global
 * exception handling system. Returns HTTP 404 status.</p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * CalendarEvent event = repository.findByPublicId(publicId)
 *     .orElseThrow(() -> new CalendarEventNotFoundException(publicId));
 * }</pre>
 * 
 * @see CalendarDomainException
 * @author Steby Corp
 */
public class CalendarEventNotFoundException extends CalendarDomainException {
    
    /**
     * Constructs exception with event public ID.
     * Status is set to 404 (Not Found).
     * 
     * @param publicId The public ID of the event that was not found
     */
    public CalendarEventNotFoundException(String publicId) {
        super("Calendar event not found: " + publicId, 404);
    }

    /**
     * Constructs exception with custom message.
     * Status is set to 404 (Not Found).
     * 
     * @param message Custom error message
     * @param ignored Ignored parameter (for overload differentiation)
     */
    public CalendarEventNotFoundException(String message, boolean ignored) {
        super(message, 404);
    }
}