package be.steby.CoreProject.bll.domains.calendar.models;

import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;
import java.time.Instant;

/**
 * Request model for creating a calendar event (BLL layer).
 * 
 * <p>Immutable record with validation in compact constructor.
 * Used to pass validated data from presentation layer to service layer.</p>
 * 
 * <h3>Validation Rules</h3>
 * <ul>
 *   <li>Title: required, non-empty</li>
 *   <li>Start date: required</li>
 *   <li>End date: required</li>
 *   <li>Recurrence: defaults to NONE if null</li>
 * </ul>
 * 
 * @param title Event title (required)
 * @param description Event description (optional)
 * @param location Event location (optional)
 * @param startDateTime Event start date/time (required)
 * @param endDateTime Event end date/time (required)
 * @param allDay Whether this is an all-day event
 * @param status Event status (defaults to CONFIRMED if null)
 * @param recurrence Recurrence pattern (defaults to NONE if null)
 * @param colorCode Hex color code for UI (optional)
 * @param reminderMinutes Minutes before event to send reminder (optional)
 * 
 * @author Steby Corp
 */
public record CalendarEventCreateRequest(
    String title,
    String description,
    String location,
    Instant startDateTime,
    Instant endDateTime,
    boolean allDay,
    EventStatus status,
    EventRecurrence recurrence,
    String colorCode,
    Integer reminderMinutes
) {
    /**
     * Compact constructor with validation.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public CalendarEventCreateRequest {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (startDateTime == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDateTime == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (recurrence == null) {
            recurrence = EventRecurrence.NONE;
        }
    }
}