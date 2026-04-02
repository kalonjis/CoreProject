package be.steby.CoreProject.bll.domains.supportticket.services;

import be.steby.CoreProject.bll.common.services.notification.mailer.BaseMailerService;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.il.mail.EmailComposer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Mailer service for public support ticket email notifications.
 *
 * <p>Sends two emails on public ticket submission:</p>
 * <ul>
 *   <li>Confirmation to the reporter (acknowledges receipt)</li>
 *   <li>Alert to the admin team (informs of new incoming ticket)</li>
 * </ul>
 */
@Service
@Slf4j
public class SupportTicketMailerService extends BaseMailerService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    @Value("${support.email.admin-recipient}")
    private String adminRecipient;

    public SupportTicketMailerService(EmailComposer emailComposer) {
        super(emailComposer);
    }

    /**
     * Sends a confirmation email to the person who submitted the ticket.
     *
     * <p>Recipient is resolved from the ticket: {@code reporterEmail} for anonymous
     * submissions, or the linked contact's email for known contacts.</p>
     *
     * @param ticket the newly created public ticket
     */
    public void sendConfirmation(SupportTicket ticket) {
        String recipientEmail = resolveReporterEmail(ticket);
        if (recipientEmail == null) {
            log.warn("No reporter email found for ticket {} — skipping confirmation", ticket.getPublicId());
            return;
        }

        String recipientName = resolveReporterName(ticket);

        Context context = new Context();
        context.setVariable("name",        recipientName);
        context.setVariable("subject",     ticket.getSubject());
        context.setVariable("publicId",    ticket.getPublicId());
        context.setVariable("submittedAt", DATE_FORMATTER.format(ticket.getCreatedAt()));

        sendEmail(
                "Votre demande de support a bien été reçue",
                "crm/support-ticket-confirmation",
                context,
                recipientEmail
        );

        log.info("Support ticket confirmation sent to {} for ticket {}", recipientEmail, ticket.getPublicId());
    }

    /**
     * Sends an alert email to the admin team about a new incoming public ticket.
     *
     * @param ticket the newly created public ticket
     */
    public void sendAdminAlert(SupportTicket ticket) {
        Context context = new Context();
        context.setVariable("publicId",      ticket.getPublicId());
        context.setVariable("subject",       ticket.getSubject());
        context.setVariable("description",   ticket.getDescription());
        context.setVariable("reporterName",  resolveReporterName(ticket));
        context.setVariable("reporterEmail", resolveReporterEmail(ticket));
        context.setVariable("submittedAt",   DATE_FORMATTER.format(ticket.getCreatedAt()));

        sendEmail(
                "[Support] Nouveau ticket : " + ticket.getSubject(),
                "crm/support-ticket-alert",
                context,
                adminRecipient
        );

        log.info("Support ticket admin alert sent to {} for ticket {}", adminRecipient, ticket.getPublicId());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String resolveReporterEmail(SupportTicket ticket) {
        if (ticket.getReporterEmail() != null) {
            return ticket.getReporterEmail();
        }
        if (ticket.getSubmittedBy() != null) {
            return ticket.getSubmittedBy().getEmail();
        }
        return null;
    }

    private String resolveReporterName(SupportTicket ticket) {
        if (ticket.getReporterName() != null) {
            return ticket.getReporterName();
        }
        if (ticket.getSubmittedBy() != null) {
            return ticket.getSubmittedBy().getFullName();
        }
        return "Utilisateur";
    }
}
