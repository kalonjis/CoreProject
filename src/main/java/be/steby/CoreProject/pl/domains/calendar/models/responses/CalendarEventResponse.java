package be.steby.CoreProject.pl.domains.calendar.models.responses;

import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;
import java.time.Instant;

/**
 * DTO for calendar event responses in REST API.
 * 
 * <p>This is the presentation layer model returned to clients.
 * Never exposes internal database IDs - only publicId is exposed.</p>
 * 
 * <h3>Security</h3>
 * <ul>
 *   <li>Internal ID (Long) never exposed</li>
 *   <li>Only publicId (UUID) exposed for API operations</li>
 *   <li>Prevents enumeration attacks</li>
 * </ul>
 * 
 * <h3>JSON Example</h3>
 * <pre>{@code
 * {
 *   "publicId": "550e8400-e29b-41d4-a716-446655440000",
 *   "title": "Team Meeting",
 *   "startDateTime": "2025-02-10T14:00:00Z",
 *   "endDateTime": "2025-02-10T15:00:00Z",
 *   "status": "CONFIRMED",
 *   ...
 * }
 * }</pre>
 * 
 * @author Steby Corp
 */
public record CalendarEventResponse(
    String publicId,
    String ownerPublicId,
    String title,
    String description,
    String location,
    Instant startDateTime,
    Instant endDateTime,
    boolean allDay,
    EventStatus status,
    EventRecurrence recurrence,
    String colorCode,
    Integer reminderMinutes,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Converts entity to DTO for API response.
     * 
     * @param event The entity to convert
     * @return DTO with all fields populated
     */
    public static CalendarEventResponse fromEntity(CalendarEvent event) {
        return new CalendarEventResponse(
            event.getPublicId(),
            event.getOwnerPublicId(),
            event.getTitle(),
            event.getDescription(),
            event.getLocation(),
            event.getStartDateTime(),
            event.getEndDateTime(),
            event.isAllDay(),
            event.getStatus(),
            event.getRecurrence(),
            event.getColorCode(),
            event.getReminderMinutes(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}