package be.steby.CoreProject.bll.domains.calendar.listeners;

import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventCreateRequest;
import be.steby.CoreProject.bll.domains.calendar.services.CalendarEventService;
import be.steby.CoreProject.bll.domains.crm.commercialaction.events.CommercialActionCancelledEvent;
import be.steby.CoreProject.bll.domains.crm.commercialaction.events.CommercialActionCreatedEvent;
import be.steby.CoreProject.bll.domains.crm.commercialaction.events.CommercialActionUpdatedEvent;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Listens to {@link CommercialAction} domain events and keeps the
 * linked {@link be.steby.CoreProject.dl.entities.CalendarEvent} in sync.
 *
 * <h3>Design (Option B)</h3>
 * <p>The {@code CalendarEvent} entity owns the reference via {@code sourceType} /
 * {@code sourcePublicId}. The {@code CommercialAction} entity has no knowledge of
 * the calendar domain. This listener is the single coupling point between the two
 * domains and lives in {@code bll/domains/calendar/listeners/} to make ownership explicit.</p>
 *
 * <h3>Conditions for calendar event creation</h3>
 * <ul>
 *   <li>Action type must return {@code true} from {@code requiresCalendarSlot()} (MEETING or DEMO)</li>
 *   <li>Action must have a {@code dueDate} set</li>
 * </ul>
 *
 * <h3>Sync on update</h3>
 * <p>When a CommercialAction is updated, the linked CalendarEvent (if any) is updated
 * with the latest title, description, location, address, start/end times, and owner
 * (to handle inline reassignment).</p>
 *
 * <h3>Cancellation propagation</h3>
 * <p>Cancelling a CommercialAction automatically cancels the linked CalendarEvent
 * (soft delete — status set to CANCELLED).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommercialActionCalendarListener {

    private static final String SOURCE_TYPE = "COMMERCIAL_ACTION";
    private static final int    DEFAULT_DURATION_MINUTES = 60;

    private final CalendarEventService calendarEventService;

    // =========================================================================
    // Creation
    // =========================================================================

    @EventListener
    @Transactional
    public void onCreated(CommercialActionCreatedEvent event) {
        CommercialAction action = event.action();

        if (!action.getType().requiresCalendarSlot() || action.getDueDate() == null) {
            return;
        }

        int duration = action.getDurationMinutes() != null
                ? action.getDurationMinutes()
                : DEFAULT_DURATION_MINUTES;

        Instant start = action.getDueDate();
        Instant end   = start.plusSeconds(duration * 60L);

        Integer reminderMinutes = computeReminderMinutes(action.getReminderAt(), start);

        CalendarEventCreateRequest request = new CalendarEventCreateRequest(
                action.getTitle(),
                action.getDescription(),
                action.getLocation(),
                action.getAddress(),
                start,
                end,
                false,
                EventStatus.CONFIRMED,
                EventRecurrence.NONE,
                null,
                reminderMinutes,
                SOURCE_TYPE,
                action.getPublicId()
        );

        calendarEventService.createEvent(request, action.getAssignedTo());
        log.debug("Calendar event created from CommercialAction: {}", action.getPublicId());
    }

    // =========================================================================
    // Update sync
    // =========================================================================

    @EventListener
    @Transactional
    public void onUpdated(CommercialActionUpdatedEvent event) {
        CommercialAction action = event.action();

        if (!action.getType().requiresCalendarSlot()) {
            return;
        }

        int duration = action.getDurationMinutes() != null
                ? action.getDurationMinutes()
                : DEFAULT_DURATION_MINUTES;

        Instant start = action.getDueDate();
        Instant end   = start != null ? start.plusSeconds(duration * 60L) : null;

        Integer reminderMinutes = computeReminderMinutes(action.getReminderAt(), start);

        calendarEventService.updateBySourcePublicId(
                action.getPublicId(),
                action.getTitle(),
                action.getDescription(),
                action.getLocation(),
                action.getAddress(),
                start,
                end,
                action.getAssignedTo() != null ? action.getAssignedTo().getPublicId() : null,
                reminderMinutes
        );
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Computes the reminder offset in minutes before the event start.
     *
     * <p>Returns {@code null} if either argument is null or if {@code reminderAt}
     * is not strictly before {@code dueDate} (negative offset makes no sense).</p>
     */
    private static Integer computeReminderMinutes(Instant reminderAt, Instant dueDate) {
        if (reminderAt == null || dueDate == null) return null;
        long minutes = Duration.between(reminderAt, dueDate).toMinutes();
        return minutes > 0 ? (int) minutes : null;
    }

    // =========================================================================
    // Cancellation propagation
    // =========================================================================

    @EventListener
    @Transactional
    public void onCancelled(CommercialActionCancelledEvent event) {
        calendarEventService.cancelBySourcePublicId(event.action().getPublicId());
    }
}
