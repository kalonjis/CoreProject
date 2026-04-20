package be.steby.CoreProject.bll.domains.crm.outreach.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;

import java.time.Instant;

/**
 * Domain event published when a CRM outreach email has been dispatched to a lead.
 *
 * <p>Mirrors {@link OutreachEmailSentEvent} but targets a {@link Lead} instead of a Contact,
 * so that the interaction is logged with {@code leadPublicId} in the timeline.</p>
 *
 * @param lead        the resolved lead the email was sent to
 * @param commercial  the CRM user who composed and sent the email (Reply-To address)
 * @param subject     the email subject line
 * @param body        the full email body
 * @param timestamp   when the event was raised
 */
public record LeadOutreachEmailSentEvent(
        Lead    lead,
        User    commercial,
        String  subject,
        String  body,
        Instant timestamp
) {
    public LeadOutreachEmailSentEvent(Lead lead, User commercial, String subject, String body) {
        this(lead, commercial, subject, body, Instant.now());
    }
}
