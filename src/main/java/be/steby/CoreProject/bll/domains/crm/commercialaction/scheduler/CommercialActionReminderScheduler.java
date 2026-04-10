package be.steby.CoreProject.bll.domains.crm.commercialaction.scheduler;

import be.steby.CoreProject.dal.repositories.NotificationPreferenceRepository;
import be.steby.CoreProject.dal.repositories.crm.CommercialActionRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import be.steby.CoreProject.il.mail.EmailComposer;
import be.steby.CoreProject.il.sse.SseNotificationPusher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scheduler that dispatches commercial action reminders to assigned commercials.
 *
 * <p>Runs every 15 minutes (configurable). For each pending action whose
 * {@code reminderAt} has arrived and {@code reminderSentAt} is still null,
 * it sends a notification via the channels enabled in the commercial's
 * {@link be.steby.CoreProject.dl.entities.NotificationPreference}.</p>
 *
 * <h3>Channel resolution</h3>
 * <p>Uses an opt-out model: both IN_APP and EMAIL are sent by default.
 * The commercial can disable a channel via their notification preferences.</p>
 *
 * <h3>Idempotence</h3>
 * <p>{@code reminderSentAt} is set after dispatch, preventing duplicate sends
 * even if the scheduler runs again before the next window.</p>
 *
 * <h3>Timing</h3>
 * <p>Uses {@code fixedDelay} — the next run starts after the previous one completes,
 * preventing overlapping executions under load. Worst-case reminder latency equals
 * the configured interval (default 1 minute).</p>
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * app:
 *   crm:
 *     reminder:
 *       enabled: true
 *       interval-ms: 60000   # 1 minute
 * }</pre>
 */
@Component
@ConditionalOnProperty(name = "app.crm.reminder.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CommercialActionReminderScheduler {

    private static final DateTimeFormatter DUE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));

    private final CommercialActionRepository   commercialActionRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SseNotificationPusher        ssePusher;
    private final EmailComposer                emailComposer;

    @Scheduled(fixedDelayString = "${app.crm.reminder.interval-ms:60000}")
    @Transactional
    public void dispatchDueReminders() {
        List<CommercialAction> due = commercialActionRepository.findDueReminders(Instant.now());

        if (due.isEmpty()) {
            log.trace("No commercial action reminders to dispatch");
            return;
        }

        log.info("Dispatching {} commercial action reminder(s)", due.size());

        for (CommercialAction action : due) {
            try {
                dispatch(action);
                action.setReminderSentAt(Instant.now());
                commercialActionRepository.save(action);
            } catch (Exception e) {
                log.error("Failed to dispatch reminder for action publicId={}: {}",
                        action.getPublicId(), e.getMessage(), e);
            }
        }
    }

    // =========================================================================
    // Private
    // =========================================================================

    private void dispatch(CommercialAction action) {
        User assignee = action.getAssignedTo();
        if (assignee == null) {
            log.warn("Action {} has no assignee, skipping reminder", action.getPublicId());
            return;
        }

        Set<NotificationChannel> disabledChannels =
                preferenceRepository.findDisabledChannelsByUserAndType(assignee, NotificationType.REMINDER);

        boolean sendInApp = !disabledChannels.contains(NotificationChannel.IN_APP);
        boolean sendEmail = !disabledChannels.contains(NotificationChannel.EMAIL);

        if (sendInApp) {
            pushSse(action, assignee);
        }
        if (sendEmail) {
            sendEmail(action, assignee);
        }

        log.info("Reminder dispatched for action '{}' (publicId={}) to {} — inApp={}, email={}",
                action.getTitle(), action.getPublicId(), assignee.getUsername(), sendInApp, sendEmail);
    }

    private void pushSse(CommercialAction action, User assignee) {
        Map<String, Object> payload = Map.of(
                "type",      "REMINDER",
                "title",     "Rappel : " + action.getTitle(),
                "body",      buildReminderBody(action),
                "actionUrl", "/crm/commercial-actions/" + action.getPublicId()
        );
        ssePusher.pushToUser(assignee.getPublicId(), payload);
    }

    private void sendEmail(CommercialAction action, User assignee) {
        Context context = new Context();
        context.setVariable("title",          "Rappel : " + action.getTitle());
        context.setVariable("previewText",    "Vous avez une action commerciale à effectuer");
        context.setVariable("userName",       assignee.getUsername());
        context.setVariable("messageContent", buildReminderBody(action));
        context.setVariable("subtitle",       action.getDueDate() != null
                ? "Échéance : " + DUE_DATE_FORMATTER.format(action.getDueDate())
                : null);
        context.setVariable("actionUrl",      null);
        context.setVariable("actionText",     null);

        emailComposer.sendMail(
                "Rappel : " + action.getTitle(),
                "emails/notification/notification-reminder",
                context,
                assignee.getEmail()
        );
    }

    private String buildReminderBody(CommercialAction action) {
        StringBuilder sb = new StringBuilder("Vous avez une action commerciale en attente : <strong>")
                .append(action.getTitle())
                .append("</strong>.");

        if (action.getDescription() != null && !action.getDescription().isBlank()) {
            sb.append("<br>").append(action.getDescription());
        }

        if (action.getDueDate() != null) {
            sb.append("<br>Échéance : ").append(DUE_DATE_FORMATTER.format(action.getDueDate()));
        }

        return sb.toString();
    }
}
