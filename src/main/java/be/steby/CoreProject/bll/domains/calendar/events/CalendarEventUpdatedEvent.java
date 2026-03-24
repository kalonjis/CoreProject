package be.steby.CoreProject.bll.domains.calendar.events;

import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Domain event emitted when a calendar event is updated by its owner.
 *
 * <p>Consumed by cross-domain listeners to propagate changes back to the
 * originating CRM entity. For example, when a {@code COMMERCIAL_ACTION}-linked
 * event is rescheduled, the {@code CommercialAction} due date is updated accordingly.</p>
 *
 * @param event     the updated calendar event entity (post-save state)
 * @param updatedBy the user who performed the update
 * @param timestamp when the update occurred
 */
public record CalendarEventUpdatedEvent(
        CalendarEvent event,
        User updatedBy,
        Instant timestamp
) {
    public CalendarEventUpdatedEvent(CalendarEvent event, User updatedBy) {
        this(event, updatedBy, Instant.now());
    }
}
