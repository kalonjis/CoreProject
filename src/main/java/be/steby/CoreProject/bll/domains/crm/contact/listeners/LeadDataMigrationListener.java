package be.steby.CoreProject.bll.domains.crm.contact.listeners;

import be.steby.CoreProject.bll.domains.crm.lead.events.LeadConvertedEvent;
import be.steby.CoreProject.dal.repositories.crm.CommercialActionRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Listener that re-links lead interactions and commercial actions to the newly
 * created contact after a lead conversion.
 *
 * <p>Runs at {@code @Order(2)}, after {@link LeadConversionListener} ({@code @Order(1)})
 * has persisted the contact. Adds a {@code contact} reference to every interaction
 * and commercial action previously linked only to the lead, so they appear in the
 * contact timeline without losing their lead context.</p>
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class LeadDataMigrationListener {

    private final ContactRepository contactRepository;
    private final InteractionRepository interactionRepository;
    private final CommercialActionRepository commercialActionRepository;

    @EventListener
    @Transactional
    public void onLeadConverted(LeadConvertedEvent event) {
        Contact contact = contactRepository.findByOriginLead_PublicId(event.lead().getPublicId()).orElse(null);
        if (contact == null) {
            log.warn("LeadDataMigrationListener — no contact found for lead {}, skipping migration",
                    event.lead().getPublicId());
            return;
        }

        long leadId = event.lead().getId();

        List<Interaction> interactions = interactionRepository.findByLeadIdOrderByOccurredAtDesc(leadId);
        interactions.forEach(i -> i.setContact(contact));
        interactionRepository.saveAll(interactions);
        log.info("LeadDataMigrationListener — {} interaction(s) linked to contact {} (from lead {})",
                interactions.size(), contact.getPublicId(), event.lead().getPublicId());

        List<CommercialAction> actions = commercialActionRepository.findByLeadIdOrderByDueDateAsc(leadId);
        actions.forEach(a -> a.setContact(contact));
        commercialActionRepository.saveAll(actions);
        log.info("LeadDataMigrationListener — {} commercial action(s) linked to contact {} (from lead {})",
                actions.size(), contact.getPublicId(), event.lead().getPublicId());
    }
}
