package be.steby.CoreProject.bll.domains.crm.outreach.events;

import be.steby.CoreProject.bll.domains.crm.outreach.services.CrmOutreachServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;

import java.time.Instant;

/**
 * Domain event published when a CRM outreach email has been dispatched to a contact.
 *
 * <p>Published by {@link CrmOutreachServiceImpl}
 * immediately after the async email send has been triggered.</p>
 *
 * <p>Listeners may react to this event to:</p>
 * <ul>
 *   <li>Log an {@code EMAIL / OUTBOUND} interaction in the contact and deal timeline
 *       (handled by {@link be.steby.CoreProject.bll.domains.outreach.listeners.OutreachInteractionListener})</li>
 *   <li>Trigger downstream notifications or activity-log entries</li>
 * </ul>
 *
 * <p>This event carries the resolved {@link Contact} entity (not just its public ID)
 * so that listeners can link the interaction without an extra DB lookup.</p>
 *
 * @param contact      the resolved contact the email was sent to
 * @param commercial   the CRM user who composed and sent the email (will be the Reply-To address)
 * @param subject      the email subject line
 * @param body         the full email body
 * @param dealPublicId public UUID of the deal to attach the log to — may be {@code null}
 * @param timestamp    when the event was raised
 */
public record OutreachEmailSentEvent(
        Contact contact,
        User    commercial,
        String  subject,
        String  body,
        String  dealPublicId,
        Instant timestamp
) {

    /**
     * Convenience constructor that captures the current timestamp automatically.
     *
     * @param contact      the resolved target contact
     * @param commercial   the CRM user who sent the email
     * @param subject      the email subject line
     * @param body         the full email body
     * @param dealPublicId public UUID of the linked deal, or {@code null}
     */
    public OutreachEmailSentEvent(Contact contact, User commercial,
                                  String subject, String body, String dealPublicId) {
        this(contact, commercial, subject, body, dealPublicId, Instant.now());
    }
}
