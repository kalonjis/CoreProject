package be.steby.CoreProject.bll.domains.gdpr.listeners;

import be.steby.CoreProject.bll.common.services.notification.mailer.MailerService;
import be.steby.CoreProject.bll.domains.gdpr.events.GdprExportReadyEvent;
import be.steby.CoreProject.bll.domains.gdpr.events.GdprExportRequestedEvent;
import be.steby.CoreProject.bll.domains.gdpr.services.notifications.GdprMailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to GDPR export domain events and triggers email notifications.
 *
 * <p>Two events are handled:
 * <ul>
 *   <li>{@link GdprExportRequestedEvent} — sends the confirmation email</li>
 *   <li>{@link GdprExportReadyEvent}     — sends the download-ready email</li>
 * </ul>
 *
 * <p>Both handlers run on the {@code emailExecutor} thread pool,
 * consistent with all other email listeners in the project.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GdprExportNotificationListener {

    private final GdprMailerService gdprMailerService;

    /**
     * Sends the export request confirmation email.
     * Triggered when the user clicks "Request export" in the UI.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleGdprExportRequested(GdprExportRequestedEvent event) {
        log.info("Sending GDPR export confirmation email to: {}", event.user().getEmail());
        try {
            gdprMailerService.sendGdprExportConfirmation(event.confirmToken(), event.user());
        } catch (Exception e) {
            log.error("Failed to send GDPR export confirmation email to: {}", event.user().getEmail(), e);
        }
    }

    /**
     * Sends the archive-ready email with the download link.
     * Triggered when async archive generation completes successfully.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleGdprExportReady(GdprExportReadyEvent event) {
        log.info("Sending GDPR export ready email to: {}", event.user().getEmail());
        try {
            gdprMailerService.sendGdprExportReady(event.downloadToken(), event.downloadUrl(), event.user());
        } catch (Exception e) {
            log.error("Failed to send GDPR export ready email to: {}", event.user().getEmail(), e);
        }
    }
}