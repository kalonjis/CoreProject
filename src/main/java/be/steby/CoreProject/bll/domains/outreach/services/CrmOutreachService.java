package be.steby.CoreProject.bll.domains.outreach.services;

import be.steby.CoreProject.bll.domains.crm.interaction.listeners.OutreachEmailInteractionListener;
import be.steby.CoreProject.bll.domains.crm.interaction.services.InteractionService;
import be.steby.CoreProject.bll.domains.outreach.models.CrmOutreachRequest;
import be.steby.CoreProject.dl.entities.User;

/**
 * Service for sending CRM outreach emails from a commercial to a contact.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Resolves the target contact by public UUID.</li>
 *   <li>Dispatches the email asynchronously via {@link CrmOutreachMailerService}
 *       (fire-and-forget with circuit breaker + retry fallback).</li>
 *   <li>Publishes {@link be.steby.CoreProject.bll.domains.outreach.events.OutreachEmailSentEvent}
 *       so that {@link be.steby.CoreProject.bll.domains.outreach.listeners.OutreachInteractionListener}
 *       can log the send as an {@code EMAIL / OUTBOUND} interaction in the timeline.</li>
 * </ol>
 *
 * <h3>Design — event-driven decoupling</h3>
 * <p>Interaction logging is handled by
 * {@link OutreachEmailInteractionListener}
 * in the {@code interaction} domain, which reacts to the published event and delegates
 * to {@link InteractionService#create}.
 * The {@code outreach} domain has zero knowledge of interaction storage (SoC / DDD).</p>
 */
public interface CrmOutreachService {

    /**
     * Sends an outreach email to the specified contact and publishes an event
     * so the send is automatically logged in the CRM timeline.
     *
     * @param request    the outreach request (contact public ID, optional deal public ID,
     *                   subject, and body)
     * @param commercial the authenticated commercial performing the send — their email
     *                   is set as the Reply-To header so replies land in their inbox
     */
    void send(CrmOutreachRequest request, User commercial);
}
