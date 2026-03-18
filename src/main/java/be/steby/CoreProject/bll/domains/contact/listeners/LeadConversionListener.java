package be.steby.CoreProject.bll.domains.contact.listeners;

import be.steby.CoreProject.bll.domains.contact.services.ContactService;
import be.steby.CoreProject.bll.domains.lead.events.LeadConvertedEvent;
import be.steby.CoreProject.dl.entities.crm.Contact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Listener that creates a {@link Contact} when a Lead is converted.
 *
 * <p>This is the primary cross-domain integration point between the lead
 * domain and the contact domain. When a commercial converts a lead, this
 * listener is responsible for materialising the resulting Contact record.</p>
 *
 * <h3>Deduplication</h3>
 * <p>If a contact with the same email already exists, the existing contact
 * is linked to the originating lead and returned — no duplicate is created.
 * This handles cases where the same person submitted multiple inquiries or
 * was already entered manually by a commercial.</p>
 *
 * <h3>Ordering</h3>
 * <p>Runs first ({@code @Order(1)}) so the Contact is persisted before any
 * downstream listeners (e.g. notifications) that may reference it.</p>
 *
 * @see be.steby.CoreProject.bll.domains.lead.events.LeadConvertedEvent
 * @see ContactService#createFromLead(be.steby.CoreProject.dl.entities.crm.Lead)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeadConversionListener {

    private final ContactService contactService;

    @EventListener
    @Order(1)
    public void onLeadConverted(LeadConvertedEvent event) {
        log.info("LeadConversionListener — creating Contact from lead publicId: {}",
                event.lead().getPublicId());

        Contact contact = contactService.createFromLead(
                event.lead(),
                event.organisationPublicId(),
                event.organisationName(),
                event.convertedBy());

        log.info("LeadConversionListener — Contact created/linked: publicId={}, email={}",
                contact.getPublicId(), contact.getEmail());
    }
}
