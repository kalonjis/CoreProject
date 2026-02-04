package be.steby.CoreProject.pl.domains.calendar.models.requests;

import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventUpdateRequest;
import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTO for updating a calendar event via REST API.
 *
 * <p>All fields are optional - only provided (non-null) fields will be updated.
 * This enables partial updates via REST API.</p>
 *
 * <h4>Address Update:</h4>
 * <p>To update the event's address, provide a complete {@code address}
 * object. The system will find or create the address as needed.</p>
 *
 * <h4>Validation:</h4>
 * <ul>
 *   <li>Title: max 200 chars (if provided)</li>
 *   <li>Description: max 5000 chars (if provided)</li>
 *   <li>Location: max 300 chars (if provided)</li>
 *   <li>Address: validated if provided</li>
 *   <li>Color code: exactly 7 chars (if provided)</li>
 * </ul>
 *
 * <h4>Example JSON:</h4>
 * <pre>{@code
 * {
 *   "title": "Updated Meeting Title",
 *   "location": "New Conference Room",
 *   "address": {
 *     "streetName": "Rue Neuve",
 *     "streetNumber": "10",
 *     "postalCode": "1000",
 *     "city": "Bruxelles",
 *     "countryCode": "BE"
 *   }
 *   // Other fields omitted = unchanged
 * }
 * }</pre>
 *
 * @author Steby Corp
 */
public record UpdateEventRequest(

        @Size(max = 200, message = "Title must be less than 200 characters")
        String title,

        @Size(max = 5000, message = "Description must be less than 5000 characters")
        String description,

        @Size(max = 300, message = "Location must be less than 300 characters")
        String location,

        @Valid
        AddressInput address,

        Instant startDateTime,

        Instant endDateTime,

        Boolean allDay,

        EventStatus status,

        EventRecurrence recurrence,

        @Size(max = 7, message = "Color code must be 7 characters")
        String colorCode,

        Integer reminderMinutes
) {

    /**
     * Converts this DTO to BLL model for service layer.
     *
     * @return BLL request model
     */
    public CalendarEventUpdateRequest toBllModel() {
        return new CalendarEventUpdateRequest(
                title,
                description,
                location,
                address != null ? address.toEntity() : null,
                startDateTime,
                endDateTime,
                allDay,
                status,
                recurrence,
                colorCode,
                reminderMinutes
        );
    }
}