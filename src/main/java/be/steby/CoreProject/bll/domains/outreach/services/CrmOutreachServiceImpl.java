package be.steby.CoreProject.bll.domains.outreach.services;

import be.steby.CoreProject.bll.domains.crm.interaction.listeners.OutreachEmailInteractionListener;
import be.steby.CoreProject.bll.domains.outreach.events.OutreachEmailSentEvent;
import be.steby.CoreProject.bll.domains.outreach.exceptions.OutreachContactNoEmailException;
import be.steby.CoreProject.bll.domains.outreach.exceptions.OutreachContactNotFoundException;
import be.steby.CoreProject.bll.domains.outreach.models.CrmOutreachRequest;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CrmOutreachService}.
 *
 * <h3>Responsibilities</h3>
 * <p>This service is intentionally slim — it only orchestrates the two
 * steps that must happen in strict order:</p>
 * <ol>
 *   <li>Resolve the target {@link Contact} by public UUID.</li>
 *   <li>Trigger the async email send via {@link CrmOutreachMailerService}.</li>
 *   <li>Publish {@link OutreachEmailSentEvent} so downstream listeners can react.</li>
 * </ol>
 *
 * <h3>Decoupling — events over direct calls</h3>
 * <p>Interaction logging is handled by
 * {@link OutreachEmailInteractionListener}
 * in the {@code interaction} domain, which reacts to {@link OutreachEmailSentEvent}
 * and delegates to {@code InteractionService}. This service therefore has
 * <em>zero knowledge</em> of the interaction domain (SoC / DDD).</p>
 *
 * <h3>Async email, event-driven log</h3>
 * <p>The SMTP dispatch is fire-and-forget (handled by the async email thread pool
 * with circuit breaker and retry). The event is published synchronously in the
 * same transaction, so the interaction log is always created regardless of
 * transient SMTP failures.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CrmOutreachServiceImpl implements CrmOutreachService {

    private final ContactRepository        contactRepository;
    private final CrmOutreachMailerService outreachMailerService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void send(CrmOutreachRequest request, User commercial) {
        Contact contact = contactRepository.findByPublicId(request.contactPublicId())
                .orElseThrow(() -> OutreachContactNotFoundException.byPublicId(request.contactPublicId()));

        if (contact.getEmail() == null || contact.getEmail().isBlank()) {
            throw OutreachContactNoEmailException.forContact(request.contactPublicId());
        }

        log.info("CRM outreach send — to: {}, by: {}, subject: '{}'",
                contact.getEmail(), commercial.getUsername(), request.subject());

        // 1. Dispatch email asynchronously (fire-and-forget, with circuit breaker + retry)
        outreachMailerService.sendOutreach(contact, request.subject(), request.body(), commercial);

        // 2. Publish event — OutreachInteractionListener will log the EMAIL interaction
        eventPublisher.publishEvent(new OutreachEmailSentEvent(
                contact,
                commercial,
                request.subject(),
                request.body(),
                request.dealPublicId()
        ));

        log.info("CRM outreach dispatched — contact: {}, event published", contact.getPublicId());
    }
}
