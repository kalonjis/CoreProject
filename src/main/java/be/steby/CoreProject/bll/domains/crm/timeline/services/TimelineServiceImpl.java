package be.steby.CoreProject.bll.domains.crm.timeline.services;

import be.steby.CoreProject.bll.domains.crm.timeline.models.TimelineEntry;
import be.steby.CoreProject.dal.repositories.crm.CommercialActionRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of {@link TimelineService}.
 *
 * <h3>Aggregation strategy</h3>
 * <p>Each timeline method fetches two lists from the DAL — one from
 * {@link InteractionRepository} and one from {@link CommercialActionRepository}
 * (filtering on {@code status = DONE}) — then merges and sorts them in memory
 * by occurrence / completion date descending.</p>
 *
 * <h3>Why in-memory sort</h3>
 * <p>Both sources have their own index-backed order queries. Merging in Java
 * avoids a UNION SQL query and keeps the repository layer simple.
 * Timeline sizes are bounded (typically &lt; 500 entries per entity),
 * so in-memory sort is acceptable.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TimelineServiceImpl implements TimelineService {

    private final InteractionRepository      interactionRepository;
    private final CommercialActionRepository commercialActionRepository;
    private final DealRepository             dealRepository;
    private final ContactRepository          contactRepository;
    private final LeadRepository             leadRepository;
    private final OrganisationRepository     organisationRepository;

    @Override
    public List<TimelineEntry> getTimelineByDeal(String dealPublicId) {
        Deal deal = dealRepository.findByPublicId(dealPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deal not found with publicId: " + dealPublicId));

        List<Long> contactIds = deal.getContactRoles().stream()
                .map(cr -> cr.getContact().getId())
                .toList();

        if (!contactIds.isEmpty()) {
            return merge(
                    interactionRepository.findByDealOrContacts(deal.getId(), contactIds),
                    commercialActionRepository.findByDealOrContactsAndStatus(
                            deal.getId(), contactIds, CommercialActionStatus.DONE)
            );
        }

        return merge(
                interactionRepository.findByDealIdOrderByOccurredAtDesc(deal.getId()),
                commercialActionRepository.findByDealIdAndStatusOrderByCompletedAtDesc(
                        deal.getId(), CommercialActionStatus.DONE)
        );
    }

    @Override
    public List<TimelineEntry> getTimelineByContact(String contactPublicId) {
        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));

        return merge(
                interactionRepository.findByContactOrContactDeal(contact.getId()),
                commercialActionRepository.findByContactOrContactDealAndStatusOrderByCompletedAtDesc(
                        contact.getId(), CommercialActionStatus.DONE)
        );
    }

    @Override
    public List<TimelineEntry> getTimelineByLead(String leadPublicId) {
        Lead lead = leadRepository.findByPublicId(leadPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lead not found with publicId: " + leadPublicId));

        return merge(
                interactionRepository.findByLeadIdOrderByOccurredAtDesc(lead.getId()),
                commercialActionRepository.findByLeadIdAndStatusOrderByCompletedAtDesc(
                        lead.getId(), CommercialActionStatus.DONE)
        );
    }

    @Override
    public List<TimelineEntry> getTimelineByOrganisation(String organisationPublicId) {
        Organisation org = organisationRepository.findByPublicId(organisationPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organisation not found with publicId: " + organisationPublicId));

        List<Long> contactIds = contactRepository.findByOrganisationId(org.getId())
                .stream()
                .map(Contact::getId)
                .toList();

        if (contactIds.isEmpty()) {
            return List.of();
        }

        return merge(
                interactionRepository.findByContactIdInOrderByOccurredAtDesc(contactIds),
                commercialActionRepository.findByContactIdInAndStatusOrderByCompletedAtDesc(
                        contactIds, CommercialActionStatus.DONE)
        );
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private List<TimelineEntry> merge(List<Interaction> interactions, List<CommercialAction> actions) {
        List<TimelineEntry> entries = new ArrayList<>(interactions.size() + actions.size());

        interactions.forEach(i -> entries.add(new TimelineEntry.InteractionEntry(i)));
        actions.forEach(a -> entries.add(new TimelineEntry.CommercialActionEntry(a)));

        entries.sort(Comparator.comparing(
                TimelineEntry::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return entries;
    }
}
