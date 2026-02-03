package be.steby.CoreProject.bll.domains.calendar.events;

import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.User;
import java.time.Instant;

/**
 * Domain event emitted when a calendar event is created.
 * 
 * <p>Can be consumed by listeners for:</p>
 * <ul>
 *   <li>Audit logging</li>
 *   <li>Email notifications</li>
 *   <li>Activity feeds</li>
 *   <li>Statistics/analytics</li>
 * </ul>
 * 
 * <h3>Example Listener</h3>
 * <pre>{@code
 * @Component
 * public class CalendarEventListener {
 *     
 *     @EventListener
 *     @Async
 *     public void handleEventCreated(CalendarEventCreatedEvent event) {
 *         log.info("Event created: {} by {}", 
 *                  event.getEventPublicId(), 
 *                  event.getCreatorUsername());
 *     }
 * }
 * }</pre>
 * 
 * @param event The created event entity
 * @param creator The user who created the event
 * @param timestamp When the event was created
 * 
 * @author Steby Corp
 */
public record CalendarEventCreatedEvent(
    CalendarEvent event,
    User creator,
    Instant timestamp
) {
    /**
     * Convenience constructor with automatic timestamp.
     */
    public CalendarEventCreatedEvent(CalendarEvent event, User creator) {
        this(event, creator, Instant.now());
    }

    /**
     * Gets the public ID of the created event.
     */
    public String getEventPublicId() {
        return event.getPublicId();
    }

    /**
     * Gets the username of the creator.
     */
    public String getCreatorUsername() {
        return creator.getUsername();
    }
}