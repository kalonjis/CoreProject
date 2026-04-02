package be.steby.CoreProject.bll.domains.supportticket.listeners;

import be.steby.CoreProject.bll.domains.supportticket.events.SupportTicketCreatedEvent;
import be.steby.CoreProject.bll.domains.supportticket.services.SupportTicketMailerService;
import be.steby.CoreProject.dl.enums.crm.SupportTicketSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener that sends email notifications when a public support ticket is created.
 *
 * <p>Only fires for tickets with source {@link SupportTicketSource#PUBLIC_FORM}.
 * Internally created tickets do not trigger these emails.</p>
 *
 * <p>Sends two emails asynchronously:</p>
 * <ul>
 *   <li>Confirmation to the reporter — acknowledges receipt</li>
 *   <li>Alert to the admin team — notifies of new incoming ticket</li>
 * </ul>
 *
 * <p>Email failures are caught and logged — they never block ticket creation.</p>
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class SupportTicketNotificationListener {

    private final SupportTicketMailerService mailerService;

    /**
     * Sends a confirmation email to the reporter when a public ticket is submitted.
     *
     * @param event the ticket created event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleReporterConfirmation(SupportTicketCreatedEvent event) {
        if (event.ticket().getSource() != SupportTicketSource.PUBLIC_FORM) {
            return;
        }

        log.debug("Sending confirmation email for public ticket {}", event.ticket().getPublicId());

        try {
            mailerService.sendConfirmation(event.ticket());
        } catch (Exception e) {
            log.error("Failed to send confirmation for ticket {} — {}",
                    event.ticket().getPublicId(), e.getMessage(), e);
        }
    }

    /**
     * Sends an alert to the admin team when a public ticket is submitted.
     *
     * @param event the ticket created event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleAdminAlert(SupportTicketCreatedEvent event) {
        if (event.ticket().getSource() != SupportTicketSource.PUBLIC_FORM) {
            return;
        }

        log.debug("Sending admin alert for public ticket {}", event.ticket().getPublicId());

        try {
            mailerService.sendAdminAlert(event.ticket());
        } catch (Exception e) {
            log.error("Failed to send admin alert for ticket {} — {}",
                    event.ticket().getPublicId(), e.getMessage(), e);
        }
    }
}
