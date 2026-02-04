package be.steby.CoreProject.bll.domains.calendar.models;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;

import java.time.Instant;

/**
 * Request model for updating a calendar event (BLL layer).
 *
 * <p>All fields are optional - only provided (non-null) fields will be updated.
 * This enables partial updates without having to send the entire event.</p>
 *
 * <h4>Address Update Behavior:</h4>
 * <ul>
 *   <li>If {@code address} is null → no change to existing address</li>
 *   <li>If {@code address} is provided → resolves via findOrCreate</li>
 * </ul>
 *
 * <h4>Usage:</h4>
 * <pre>{@code
 * // Update only title and location
 * var request = new CalendarEventUpdateRequest(
 *     "New Title",  // title
 *     null,         // description unchanged
 *     "New Room",   // location
 *     null,         // address unchanged
 *     null,         // dates unchanged
 *     null, null, null, null, null, null
 * );
 * }</pre>
 *
 * @param title           new event title (null = no change)
 * @param description     new description (null = no change)
 * @param location        new location text (null = no change)
 * @param address         new address (null = no change)
 * @param startDateTime   new start date/time (null = no change)
 * @param endDateTime     new end date/time (null = no change)
 * @param allDay          new all-day flag (null = no change)
 * @param status          new status (null = no change)
 * @param recurrence      new recurrence pattern (null = no change)
 * @param colorCode       new color code (null = no change)
 * @param reminderMinutes new reminder time (null = no change)
 *
 * @see be.steby.CoreProject.bll.domains.calendar.services.CalendarEventService#updateEvent
 */
public record CalendarEventUpdateRequest(
        String title,
        String description,
        String location,
        Address address,
        Instant startDateTime,
        Instant endDateTime,
        Boolean allDay,
        EventStatus status,
        EventRecurrence recurrence,
        String colorCode,
        Integer reminderMinutes
) {
}