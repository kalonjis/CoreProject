package be.steby.CoreProject.bll.domains.calendar.models;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;

import java.time.Instant;

/**
 * Request model for creating a calendar event (BLL layer).
 *
 * <p>Immutable record with validation in compact constructor.
 * Used to pass validated data from presentation layer to service layer.</p>
 *
 * <h4>Location Handling:</h4>
 * <p>Events support two complementary location mechanisms:</p>
 * <ul>
 *   <li>{@code location} - Free-text description (e.g., "Room A", "Teams")</li>
 *   <li>{@code address} - Structured physical address entity</li>
 * </ul>
 * <p>Both can be used together, separately, or neither.</p>
 *
 * <h4>Address Resolution:</h4>
 * <p>When an {@code address} is provided, the service will:</p>
 * <ol>
 *   <li>Check for existing duplicates using {@code AddressService.findOrCreate()}</li>
 *   <li>Reuse existing address if found, or create new one</li>
 *   <li>Trigger async geocoding for new addresses</li>
 * </ol>
 *
 * <h4>Validation Rules:</h4>
 * <ul>
 *   <li>Title: required, non-empty</li>
 *   <li>Start date: required</li>
 *   <li>End date: required</li>
 *   <li>Recurrence: defaults to NONE if null</li>
 * </ul>
 *
 * @param title           event title (required)
 * @param description     event description (optional)
 * @param location        free-text location description (optional)
 * @param address         structured physical address (optional)
 * @param startDateTime   event start date/time (required)
 * @param endDateTime     event end date/time (required)
 * @param allDay          whether this is an all-day event
 * @param status          event status (defaults to CONFIRMED if null)
 * @param recurrence      recurrence pattern (defaults to NONE if null)
 * @param colorCode       hex color code for UI (optional)
 * @param reminderMinutes minutes before event to send reminder (optional)
 * @param sourceType      type of the CRM source entity (e.g. "COMMERCIAL_ACTION") — null for manual events
 * @param sourcePublicId  public UUID of the CRM source entity — null for manual events
 *
 * @see be.steby.CoreProject.bll.domains.calendar.services.CalendarEventService#createEvent
 * @see be.steby.CoreProject.dl.entities.CalendarEvent
 */
public record CalendarEventCreateRequest(
        String title,
        String description,
        String location,
        Address address,
        Instant startDateTime,
        Instant endDateTime,
        boolean allDay,
        EventStatus status,
        EventRecurrence recurrence,
        String colorCode,
        Integer reminderMinutes,
        String sourceType,
        String sourcePublicId
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