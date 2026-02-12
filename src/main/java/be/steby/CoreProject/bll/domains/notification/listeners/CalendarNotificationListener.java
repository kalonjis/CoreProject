package be.steby.CoreProject.bll.domains.notification.listeners;

import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCancelledEvent;
import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCreatedEvent;
import be.steby.CoreProject.bll.domains.notification.models.NotificationRequest;
import be.steby.CoreProject.bll.domains.notification.services.NotificationService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Listener for calendar domain events that creates notifications.
 *
 * <p>This listener bridges the Calendar domain with the Notification domain,
 * creating appropriate notifications when calendar events occur.</p>
 *
 * <h4>Handled Events:</h4>
 * <ul>
 *   <li>{@link CalendarEventCreatedEvent} → Schedule reminder notification</li>
 *   <li>{@link CalendarEventCancelledEvent} → Cancel scheduled reminders</li>
 * </ul>
 *
 * <h4>Reminder Logic:</h4>
 * <p>When a calendar event is created with {@code reminderMinutes} set,
 * a scheduled notification is created that will be delivered at:</p>
 * <pre>
 *   deliveryTime = event.startDateTime - reminderMinutes
 * </pre>
 *
 * <h4>Source Tracking:</h4>
 * <p>All notifications created include source tracking:</p>
 * <ul>
 *   <li>{@code sourceDomain = "calendar"}</li>
 *   <li>{@code sourceReferenceId = event.publicId}</li>
 * </ul>
 * <p>This allows bulk deletion when the calendar event is cancelled.</p>
 *
 * @see CalendarEventCreatedEvent
 * @see CalendarEventCancelledEvent
 * @see NotificationService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarNotificationListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private static final String SOURCE_DOMAIN = "calendar";

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    // =========================================================================
    // Event Created
    // =========================================================================

    /**
     * Handles calendar event creation.
     *
     * <p>If the event has {@code reminderMinutes} configured, schedules
     * a reminder notification to be sent before the event starts.</p>
     *
     * @param event the calendar event created event
     */
    @EventListener
    @Async("notificationExecutor")
    @Order(20)
    public void handleCalendarEventCreated(CalendarEventCreatedEvent event) {
        CalendarEvent calendarEvent = event.event();
        User owner = event.creator();

        log.debug("Processing calendar event created: {} for user {}",
                calendarEvent.getPublicId(), owner.getPublicId());

        // Schedule reminder if configured
        if (calendarEvent.getReminderMinutes() != null && calendarEvent.getReminderMinutes() > 0) {
            scheduleReminderNotification(calendarEvent, owner);
        }
    }

    /**
     * Schedules a reminder notification for the calendar event.
     */
    private void scheduleReminderNotification(CalendarEvent calendarEvent, User owner) {
        Instant reminderTime = calendarEvent.getStartDateTime()
                .minus(calendarEvent.getReminderMinutes(), ChronoUnit.MINUTES);

        // Don't schedule if reminder time is already past
        if (reminderTime.isBefore(Instant.now())) {
            log.debug("Reminder time already passed for event {}, skipping",
                    calendarEvent.getPublicId());
            return;
        }

        String title = buildReminderTitle(calendarEvent);
        String body = buildReminderBody(calendarEvent);
        String actionUrl = "/calendar/events/" + calendarEvent.getPublicId();

        NotificationRequest request = NotificationRequest.builder()
                .recipient(owner)
                .type(NotificationType.REMINDER)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .scheduledFor(reminderTime)
                .expiresAt(calendarEvent.getStartDateTime().plus(1, ChronoUnit.HOURS))
                .source(SOURCE_DOMAIN, calendarEvent.getPublicId())
                .build();

        notificationService.send(request);

        log.info("Reminder scheduled for event {} at {}",
                calendarEvent.getPublicId(), reminderTime);
    }

    // =========================================================================
    // Event Cancelled
    // =========================================================================

    /**
     * Handles calendar event cancellation.
     *
     * <p>Deletes any scheduled notifications for the cancelled event
     * and optionally notifies the owner if cancelled by someone else.</p>
     *
     * @param event the calendar event cancelled event
     */
    @EventListener
    @Async("notificationExecutor")
    @Order(20)
    public void handleCalendarEventCancelled(CalendarEventCancelledEvent event) {
        CalendarEvent calendarEvent = event.event();

        log.debug("Processing calendar event cancelled: {}", calendarEvent.getPublicId());

        // Delete scheduled reminders for this event
        int deleted = notificationService.deleteBySource(SOURCE_DOMAIN, calendarEvent.getPublicId());

        if (deleted > 0) {
            log.info("Deleted {} scheduled notification(s) for cancelled event {}",
                    deleted, calendarEvent.getPublicId());
        }

        // Notify owner if cancelled by someone else (admin, shared calendar scenario)
        notifyIfCancelledByOther(event);
    }

    /**
     * Notifies the event owner if the event was cancelled by someone else.
     */
    private void notifyIfCancelledByOther(CalendarEventCancelledEvent event) {
        CalendarEvent calendarEvent = event.event();
        User cancelledBy = event.cancelledBy();

        // Find the event owner
        User owner = userRepository.findByPublicId(calendarEvent.getOwnerPublicId())
                .orElse(null);

        if (owner == null) {
            log.warn("Could not find owner for cancelled event: {}", calendarEvent.getPublicId());
            return;
        }

        // Only notify if someone else cancelled the event
        if (owner.getPublicId().equals(cancelledBy.getPublicId())) {
            return; // Owner cancelled their own event, no need to notify
        }

        String title = "Événement annulé";
        String body = String.format("L'événement \"%s\" a été annulé par %s",
                calendarEvent.getTitle(),
                cancelledBy.getUsername());

        NotificationRequest request = NotificationRequest.builder()
                .recipient(owner)
                .type(NotificationType.ALERT)
                .title(title)
                .body(body)
                .actionUrl("/calendar")
                .source(SOURCE_DOMAIN, calendarEvent.getPublicId())
                .build();

        notificationService.send(request);

        log.info("Owner {} notified of event cancellation by {}",
                owner.getPublicId(), cancelledBy.getPublicId());
    }

    // =========================================================================
    // Message Builders
    // =========================================================================

    /**
     * Builds the reminder notification title.
     */
    private String buildReminderTitle(CalendarEvent event) {
        int minutes = event.getReminderMinutes();

        if (minutes < 60) {
            return String.format("Dans %d min : %s", minutes, event.getTitle());
        } else if (minutes == 60) {
            return String.format("Dans 1 heure : %s", event.getTitle());
        } else if (minutes < 1440) {
            return String.format("Dans %d heures : %s", minutes / 60, event.getTitle());
        } else {
            return String.format("Demain : %s", event.getTitle());
        }
    }

    /**
     * Builds the reminder notification body.
     */
    private String buildReminderBody(CalendarEvent event) {
        StringBuilder body = new StringBuilder();

        // Time
        if (event.isAllDay()) {
            body.append("Toute la journée");
        } else {
            body.append("À ").append(TIME_FORMATTER.format(event.getStartDateTime()));
        }

        // Location
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            body.append(" • ").append(event.getLocation());
        }

        // Description preview
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            String preview = event.getDescription();
            if (preview.length() > 100) {
                preview = preview.substring(0, 97) + "...";
            }
            body.append("\n").append(preview);
        }

        return body.toString();
    }
}