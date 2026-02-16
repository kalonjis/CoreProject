package be.steby.CoreProject.bll.domains.calendar.listeners;

import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCancelledEvent;
import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCreatedEvent;
import be.steby.CoreProject.bll.domains.notification.models.NotificationRequest;
import be.steby.CoreProject.bll.domains.notification.services.NotificationService;
import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarNotificationListener {

    private final NotificationService notificationService;

    private static final String SOURCE_DOMAIN = "calendar";

    private static final long MINIMUM_SCHEDULE_AHEAD_SECONDS = 30;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    @EventListener
    @Async("notificationExecutor")
    @Order(20)
    public void handleCalendarEventCreated(CalendarEventCreatedEvent event) {
        CalendarEvent calendarEvent = event.event();
        User owner = event.creator();

        log.info("Processing calendar event created: {} for user {} (reminderMinutes: {})",
                calendarEvent.getPublicId(),
                owner.getPublicId(),
                calendarEvent.getReminderMinutes());

        try {
            if (calendarEvent.getReminderMinutes() != null && calendarEvent.getReminderMinutes() > 0) {
                scheduleReminderNotification(calendarEvent, owner);
            } else {
                log.debug("No reminder configured for event {}", calendarEvent.getPublicId());
            }
        } catch (Exception e) {
            log.error("Failed to schedule reminder for event {}: {}",
                    calendarEvent.getPublicId(), e.getMessage(), e);
            // TODO: Optionnel - envoyer une alerte ou retry
        }
    }

    private void scheduleReminderNotification(CalendarEvent calendarEvent, User owner) {
        Instant now = Instant.now();
        Instant reminderTime = calendarEvent.getStartDateTime()
                .minus(calendarEvent.getReminderMinutes(), ChronoUnit.MINUTES);

        log.info("Calculating reminder for event {}: startDateTime={}, reminderMinutes={}, reminderTime={}, now={}",
                calendarEvent.getPublicId(),
                calendarEvent.getStartDateTime(),
                calendarEvent.getReminderMinutes(),
                reminderTime,
                now);

        if (reminderTime.isBefore(now.plusSeconds(MINIMUM_SCHEDULE_AHEAD_SECONDS))) {
            if (reminderTime.isBefore(now.minusSeconds(60))) {
                log.warn("Reminder time {} is too far in the past for event {}, skipping",
                        reminderTime, calendarEvent.getPublicId());
                return;
            }
            log.info("Reminder time {} is very close/past for event {}, sending immediately",
                    reminderTime, calendarEvent.getPublicId());
            reminderTime = null; // null = immediate
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
                .scheduledFor(reminderTime)  // null if immediate
                .expiresAt(calendarEvent.getStartDateTime().plus(1, ChronoUnit.HOURS))
                .source(SOURCE_DOMAIN, calendarEvent.getPublicId())
                .build();

        Notification notification = notificationService.send(request);

        if (notification != null && notification.getPublicId() != null) {
            if (reminderTime != null) {
                log.info("✅ Reminder SCHEDULED for event {} at {} (notification: {})",
                        calendarEvent.getPublicId(), reminderTime, notification.getPublicId());
            } else {
                log.info("✅ Reminder SENT IMMEDIATELY for event {} (notification: {})",
                        calendarEvent.getPublicId(), notification.getPublicId());
            }
        } else {
            log.error("❌ Failed to create notification for event {} - notification is null",
                    calendarEvent.getPublicId());
        }
    }

    private String buildReminderTitle(CalendarEvent event) {
        return "📅 " + event.getTitle();
    }

    private String buildReminderBody(CalendarEvent event) {
        String time = TIME_FORMATTER.format(event.getStartDateTime());
        if (event.getReminderMinutes() < 60) {
            return String.format("Commence dans %d minutes (à %s)",
                    event.getReminderMinutes(), time);
        } else {
            return String.format("Commence dans %d heure(s) (à %s)",
                    event.getReminderMinutes() / 60, time);
        }
    }

    @EventListener
    @Async("notificationExecutor")
    @Order(20)
    public void handleCalendarEventCancelled(CalendarEventCancelledEvent event) {
        String eventPublicId = event.event().getPublicId();
        log.info("Calendar event cancelled: {}, removing scheduled notifications", eventPublicId);

        try {
            int deleted = notificationService.deleteBySource(SOURCE_DOMAIN, eventPublicId);
            log.info("Deleted {} scheduled notification(s) for cancelled event {}",
                    deleted, eventPublicId);
        } catch (Exception e) {
            log.error("Failed to delete notifications for cancelled event {}: {}",
                    eventPublicId, e.getMessage(), e);
        }
    }
}