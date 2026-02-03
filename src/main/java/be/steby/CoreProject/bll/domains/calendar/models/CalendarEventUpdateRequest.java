package be.steby.CoreProject.bll.domains.calendar.models;

import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;
import java.time.Instant;

/**
 * Request model for updating a calendar event (BLL layer).
 * 
 * <p>All fields are optional - only provided (non-null) fields will be updated.
 * This enables partial updates without having to send the entire event.</p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * // Update only title and location
 * var request = new CalendarEventUpdateRequest(
 *     "New Title",  // title
 *     null,         // description unchanged
 *     "New Room",   // location
 *     null,         // dates unchanged
 *     null, null, null, null, null, null
 * );
 * }</pre>
 * 
 * @param title New event title (null = no change)
 * @param description New description (null = no change)
 * @param location New location (null = no change)
 * @param startDateTime New start date/time (null = no change)
 * @param endDateTime New end date/time (null = no change)
 * @param allDay New all-day flag (null = no change)
 * @param status New status (null = no change)
 * @param recurrence New recurrence pattern (null = no change)
 * @param colorCode New color code (null = no change)
 * @param reminderMinutes New reminder time (null = no change)
 * 
 * @author Steby Corp
 */
public record CalendarEventUpdateRequest(
    String title,
    String description,
    String location,
    Instant startDateTime,
    Instant endDateTime,
    Boolean allDay,
    EventStatus status,
    EventRecurrence recurrence,
    String colorCode,
    Integer reminderMinutes
) {
}