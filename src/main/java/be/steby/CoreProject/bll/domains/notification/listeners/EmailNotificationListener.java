package be.steby.CoreProject.bll.domains.notification.listeners;

import be.steby.CoreProject.bll.domains.notification.events.NotificationCreatedEvent;
import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import be.steby.CoreProject.il.mail.EmailComposer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

/**
 * Listener for email notification delivery.
 *
 * <p>Handles notification delivery via email. This listener is <b>asynchronous</b>
 * to avoid blocking the main thread, as email delivery can take 100ms to several
 * seconds depending on the SMTP server.</p>
 *
 * <h4>Why Asynchronous?</h4>
 * <ul>
 *   <li>Email delivery is slow (external SMTP server)</li>
 *   <li>Non-critical for immediate UX (user has in-app notification)</li>
 *   <li>Fire-and-forget pattern: no need to wait for result</li>
 * </ul>
 *
 * <h4>Email Templates:</h4>
 * <p>Uses Thymeleaf templates located in {@code resources/templates/mail/notification/}:</p>
 * <ul>
 *   <li>{@code notification-generic.html} - Default template</li>
 *   <li>{@code notification-security.html} - Security alerts</li>
 *   <li>{@code notification-reminder.html} - Reminders</li>
 * </ul>
 *
 * <h4>Execution Order:</h4>
 * <p>Uses {@code @Order(10)} to execute after synchronous listeners like
 * {@link InAppNotificationListener}.</p>
 *
 * @see NotificationCreatedEvent
 * @see EmailComposer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailComposer emailComposer;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    @Value("${spring.mail.enabled:true}")
    private boolean mailEnabled;

    // =========================================================================
    // Template Paths
    // =========================================================================

    private static final String TEMPLATE_BASE = "mail/notification/";
    private static final String TEMPLATE_GENERIC = TEMPLATE_BASE + "notification-generic";
    private static final String TEMPLATE_SECURITY = TEMPLATE_BASE + "notification-security";
    private static final String TEMPLATE_REMINDER = TEMPLATE_BASE + "notification-reminder";

    /**
     * Handles notification delivery via email.
     *
     * <p>This method is asynchronous (@Async) and executes after in-app delivery
     * (@Order(10)). Email failures do not affect other notification channels.</p>
     *
     * @param event the notification created event
     */
    @EventListener
    @Async("emailExecutor")
    @Order(10)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        // Check if EMAIL channel is requested
        if (!event.shouldDeliverVia(NotificationChannel.EMAIL)) {
            return;
        }

        // Check if mail is enabled
        if (!mailEnabled) {
            log.debug("Email disabled, skipping email notification {}",
                    event.getNotificationPublicId());
            return;
        }

        Notification notification = event.notification();
        User recipient = notification.getRecipient();

        // Validate recipient has email
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            log.warn("Cannot send email notification {}: user {} has no email",
                    event.getNotificationPublicId(), event.getRecipientPublicId());
            return;
        }

        log.debug("Sending email notification {} to {}",
                event.getNotificationPublicId(), recipient.getEmail());

        try {
            Context context = buildEmailContext(notification);
            String template = selectTemplate(notification.getType());
            String subject = buildSubject(notification);

            //TODO check si c'etait bien sendMail ou une autre methode
            emailComposer.sendMail(
                    subject,
                    template,
                    context,
                    recipient.getEmail()
            );

            log.debug("Email notification {} sent to {}",
                    event.getNotificationPublicId(), recipient.getEmail());

        } catch (Exception e) {
            log.error("Failed to send email notification {} to {}: {}",
                    event.getNotificationPublicId(), recipient.getEmail(), e.getMessage());
            // Don't throw - email failure should not affect other operations
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Builds the Thymeleaf context for the email template.
     */
    private Context buildEmailContext(Notification notification) {
        Context context = new Context();

        // User info
        User recipient = notification.getRecipient();
        context.setVariable("userName", recipient.getFirstname() != null
                ? recipient.getFirstname()
                : recipient.getUsername());

        // Notification info
        context.setVariable("title", notification.getTitle());
        context.setVariable("body", notification.getBody());
        context.setVariable("type", notification.getType());
        context.setVariable("priority", notification.getPriority());
        context.setVariable("createdAt", notification.getCreatedAt());

        // Action URL
        if (notification.getActionUrl() != null) {
            String fullUrl = notification.getActionUrl().startsWith("http")
                    ? notification.getActionUrl()
                    : appBaseUrl + notification.getActionUrl();
            context.setVariable("actionUrl", fullUrl);
            context.setVariable("hasAction", true);
        } else {
            context.setVariable("hasAction", false);
        }

        // App info
        context.setVariable("appBaseUrl", appBaseUrl);

        return context;
    }

    /**
     * Selects the appropriate email template based on notification type.
     */
    private String selectTemplate(NotificationType type) {
        return switch (type) {
            case SECURITY -> TEMPLATE_SECURITY;
            case REMINDER -> TEMPLATE_REMINDER;
            default -> TEMPLATE_GENERIC;
        };
    }

    /**
     * Builds the email subject line.
     */
    private String buildSubject(Notification notification) {
        String prefix = switch (notification.getType()) {
            case SECURITY -> "🔒 Security Alert: ";
            case REMINDER -> "⏰ Reminder: ";
            case ALERT -> "⚠️ Alert: ";
            default -> "";
        };

        return prefix + notification.getTitle();
    }
}