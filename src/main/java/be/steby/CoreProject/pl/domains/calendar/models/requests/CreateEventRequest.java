package be.steby.CoreProject.pl.domains.calendar.models.requests;

import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventCreateRequest;
import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTO for creating a calendar event via REST API.
 *
 * <p>This is the presentation layer model with Jakarta validation.
 * Converts to BLL model ({@link CalendarEventCreateRequest}) via {@link #toBllModel()}.</p>
 *
 * <h4>Location Options:</h4>
 * <p>The API supports flexible location specification:</p>
 * <ul>
 *   <li><b>Free-text only:</b> Set {@code location}, leave {@code address} null</li>
 *   <li><b>Address only:</b> Set {@code address}, leave {@code location} null</li>
 *   <li><b>Both:</b> Set both for context + physical location</li>
 *   <li><b>Neither:</b> For events without specific location</li>
 * </ul>
 *
 * <h4>Validation:</h4>
 * <ul>
 *   <li>Title: required, max 200 chars</li>
 *   <li>Description: optional, max 5000 chars</li>
 *   <li>Location: optional, max 300 chars</li>
 *   <li>Address: optional, validated if provided</li>
 *   <li>Start/End dates: required</li>
 *   <li>Color code: optional, exactly 7 chars (#RRGGBB)</li>
 * </ul>
 *
 * @author Steby Corp
 */
public record CreateEventRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be less than 200 characters")
        String title,

        @Size(max = 5000, message = "Description must be less than 5000 characters")
        String description,

        @Size(max = 300, message = "Location must be less than 300 characters")
        String location,

        @Valid
        AddressInput address,

        @NotNull(message = "Start date/time is required")
        Instant startDateTime,

        @NotNull(message = "End date/time is required")
        Instant endDateTime,

        boolean allDay,

        EventStatus status,

        EventRecurrence recurrence,

        @Size(max = 7, message = "Color code must be 7 characters (e.g., #FF5733)")
        String colorCode,

        Integer reminderMinutes
) {

    /**
     * Converts this DTO to BLL model for service layer.
     *
     * @return BLL request model
     */
    public CalendarEventCreateRequest toBllModel() {
        return new CalendarEventCreateRequest(
                title,
                description,
                location,
                address != null ? address.toEntity() : null,
                startDateTime,
                endDateTime,
                allDay,
                status != null ? status : EventStatus.CONFIRMED,
                recurrence != null ? recurrence : EventRecurrence.NONE,
                colorCode,
                reminderMinutes,
                null,
                null
        );
    }
}