package be.steby.CoreProject.bll.domains.lead.listeners;

import be.steby.CoreProject.bll.domains.lead.events.LeadSubmittedEvent;
import be.steby.CoreProject.bll.domains.lead.services.LeadMailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for public lead submission events.
 *
 * <p>Handles email notifications when a public lead is submitted:</p>
 * <ul>
 *   <li>Sends notification to the appropriate team</li>
 *   <li>Sends acknowledgment to the visitor</li>
 * </ul>
 *
 * <p>Email delivery failures are logged but do not affect the submission process.</p>
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class LeadNotificationListener {

    private final LeadMailerService leadMailerService;

    /**
     * Handles lead submission by sending notification to the team.
     *
     * @param event the lead submitted event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleLeadNotification(LeadSubmittedEvent event) {
        log.debug("Handling leadSubmittedEvent - notification for: {}",
                event.lead().getPublicId());

        try {
            leadMailerService.sendLeadNotification(event);

            log.info("lead notification sent successfully for: {}",
                    event.lead().getPublicId());

        } catch (Exception e) {
            log.error("Failed to send lead notification for: {} - Error: {}",
                    event.lead().getPublicId(), e.getMessage(), e);
        }
    }

    /**
     * Handles lead submission by sending acknowledgment to the visitor.
     *
     * @param event the lead submitted event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleInquiryAcknowledgment(LeadSubmittedEvent event) {
        log.debug("Handling LeadSubmittedEvent - acknowledgment for: {}",
                event.lead().getPublicId());

        try {
            leadMailerService.sendLeadAcknowledgment(event);

            log.info("Lead acknowledgment sent successfully to: {}",
                    event.getReplyToEmail());

        } catch (Exception e) {
            log.error("Failed to send lead acknowledgment to: {} - Error: {}",
                    event.getReplyToEmail(), e.getMessage(), e);
        }
    }
}