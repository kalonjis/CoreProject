package be.steby.CoreProject.bll.domains.lead.services;

import be.steby.CoreProject.bll.common.services.notification.mailer.BaseMailerService;
import be.steby.CoreProject.bll.domains.lead.events.LeadSubmittedEvent;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.il.mail.EmailComposer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Lead domain-specific mailer service.
 *
 * <p>Handles email notifications for public lead submissions.
 * Routes emails to the appropriate team based on {@link LeadType}.</p>
 */
@Service
@Slf4j
public class LeadMailerService extends BaseMailerService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    @Value("${lead.email.default-recipient}")
    private String defaultRecipient;

    @Value("${lead.email.recipients.commercial:#{null}}")
    private String commercialRecipient;

    @Value("${lead.email.recipients.partnership:#{null}}")
    private String partnershipRecipient;

    @Value("${lead.email.recipients.press:#{null}}")
    private String pressRecipient;

    public LeadMailerService(EmailComposer emailComposer) {
        super(emailComposer);
    }

    /**
     * Sends lead notification to the appropriate team.
     *
     * @param event the lead submitted event
     */
    public void sendLeadNotification(LeadSubmittedEvent event) {
        String recipient = resolveRecipient(event.lead().getLeadType());

        log.info("Sending lead notification to {} for lead: {}",
                recipient, event.lead().getPublicId());

        Context context = createInquiryContext(event);

        sendEmail(
                event.getEmailSubject(),
                "lead/leadNotification",
                context,
                recipient
        );

        log.debug("lead notification email sent successfully for: {}",
                event.lead().getPublicId());
    }

    /**
     * Sends acknowledgment email to the visitor.
     *
     * @param event the lead submitted event
     */
    public void sendLeadAcknowledgment(LeadSubmittedEvent event) {
        log.info("Sending lead acknowledgment to: {}", event.getReplyToEmail());

        Context context = new Context();
        context.setVariable("name", event.getVisitorName());
        context.setVariable("subject", event.lead().getSubject());
        context.setVariable("inquiryType", formatInquiryType(event.lead().getLeadType()));
        context.setVariable("referenceId", event.lead().getPublicId());

        sendEmail(
                "We received your lead",
                "lead/leadAcknowledgment",
                context,
                event.getReplyToEmail()
        );

        log.debug("Lead acknowledgment email sent successfully to: {}", event.getReplyToEmail());
    }

    // ========================================
    // Private Methods
    // ========================================

    private Context createInquiryContext(LeadSubmittedEvent event) {
        Context context = new Context();

        context.setVariable("referenceId", event.lead().getPublicId());
        context.setVariable("visitorName", event.getVisitorName());
        context.setVariable("visitorEmail", event.getReplyToEmail());
        context.setVariable("subject", event.lead().getSubject());
        context.setVariable("message", event.message());
        context.setVariable("inquiryType", formatInquiryType(event.lead().getLeadType()));
        context.setVariable("submittedAt", DATE_FORMATTER.format(event.lead().getSubmittedAt()));
        context.setVariable("ipAddress", event.lead().getIpAddress());

        return context;
    }

    private String resolveRecipient(LeadType leadType) {
        return switch (leadType) {
            case COMMERCIAL -> commercialRecipient != null ? commercialRecipient : defaultRecipient;
            case PARTNERSHIP -> partnershipRecipient != null ? partnershipRecipient : defaultRecipient;
            case PRESS -> pressRecipient != null ? pressRecipient : defaultRecipient;
            default -> defaultRecipient;
        };
    }

    private String formatInquiryType(LeadType leadType) {
        return switch (leadType) {
            case GENERAL -> "General Lead";
            case COMMERCIAL -> "Commercial / Sales";
            case PARTNERSHIP -> "Partnership";
            case PRESS -> "Press / Media";
            case OTHER -> "Other";
        };
    }
}