package be.steby.CoreProject.bll.domains.calendar.events;

import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.User;
import java.time.Instant;

/**
 * Domain event emitted when a calendar event is cancelled.
 * 
 * <p>Can be consumed by listeners for:</p>
 * <ul>
 *   <li>Sending cancellation notifications</li>
 *   <li>Audit trail logging</li>
 *   <li>Calendar sync updates</li>
 *   <li>Analytics tracking</li>
 * </ul>
 * 
 * <h3>Example Listener</h3>
 * <pre>{@code
 * @EventListener
 * @Async
 * public void handleEventCancelled(CalendarEventCancelledEvent event) {
 *     emailService.sendCancellationNotice(
 *         event.getCancelledByUsername(),
 *         event.event()
 *     );
 * }
 * }</pre>
 * 
 * @param event The cancelled event entity
 * @param cancelledBy The user who cancelled the event
 * @param timestamp When the cancellation occurred
 * 
 * @author Steby Corp
 */
public record CalendarEventCancelledEvent(
    CalendarEvent event,
    User cancelledBy,
    Instant timestamp
) {
    /**
     * Convenience constructor with automatic timestamp.
     */
    public CalendarEventCancelledEvent(CalendarEvent event, User cancelledBy) {
        this(event, cancelledBy, Instant.now());
    }

    /**
     * Gets the public ID of the cancelled event.
     */
    public String getEventPublicId() {
        return event.getPublicId();
    }

    /**
     * Gets the username of the user who cancelled the event.
     */
    public String getCancelledByUsername() {
        return cancelledBy.getUsername();
    }
}