package be.steby.CoreProject.bll.domains.crm.contact.listeners;

import be.steby.CoreProject.bll.domains.crm.deal.events.DealWonEvent;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealContactRoleRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.DealContactRole;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import be.steby.CoreProject.dl.enums.crm.OrganisationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes the primary contact and linked organisation of a won deal to CLIENT status.
 *
 * <p>When a deal is marked WON:</p>
 * <ul>
 *   <li>The contact flagged as {@code primary = true} in {@code DealContactRole}
 *       is promoted to {@link ContactStatus#CLIENT}.</li>
 *   <li>The organisation linked to the deal (if any) is promoted to
 *       {@link OrganisationStatus#CLIENT}.</li>
 * </ul>
 *
 * <p>Both upgrades are idempotent — already-CLIENT entities are silently skipped.
 * Missing primary contact or missing organisation are also silently skipped:
 * incomplete CRM records should not prevent the deal win from completing.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DealWonContactUpgradeListener {

    private final DealContactRoleRepository dealContactRoleRepository;
    private final ContactRepository         contactRepository;
    private final OrganisationRepository    organisationRepository;

    @EventListener
    @Transactional
    public void onDealWon(DealWonEvent event) {
        Long dealId = event.deal().getId();

        DealContactRole primaryRole = dealContactRoleRepository.findPrimaryByDealId(dealId).orElse(null);
        if (primaryRole == null) {
            log.debug("DealWonContactUpgradeListener — no primary contact on deal {}, skipping upgrade",
                    event.deal().getPublicId());
            return;
        }

        Contact contact = primaryRole.getContact();
        if (contact.getStatus() == ContactStatus.CLIENT) {
            log.debug("DealWonContactUpgradeListener — contact {} is already CLIENT, skipping",
                    contact.getPublicId());
            return;
        }

        ContactStatus previous = contact.getStatus();
        contact.setStatus(ContactStatus.CLIENT);
        contactRepository.save(contact);

        log.info("DealWonContactUpgradeListener — contact {} promoted {} → CLIENT (deal {})",
                contact.getPublicId(), previous, event.deal().getPublicId());

        // ── Organisation upgrade ──────────────────────────────────────────────
        Organisation organisation = event.deal().getOrganisation();
        if (organisation == null) {
            log.debug("DealWonContactUpgradeListener — no organisation on deal {}, skipping org upgrade",
                    event.deal().getPublicId());
            return;
        }

        if (organisation.getStatus() == OrganisationStatus.CLIENT) {
            log.debug("DealWonContactUpgradeListener — organisation {} is already CLIENT, skipping",
                    organisation.getPublicId());
            return;
        }

        OrganisationStatus previousOrgStatus = organisation.getStatus();
        organisation.setStatus(OrganisationStatus.CLIENT);
        organisationRepository.save(organisation);

        log.info("DealWonContactUpgradeListener — organisation {} promoted {} → CLIENT (deal {})",
                organisation.getPublicId(), previousOrgStatus, event.deal().getPublicId());
    }
}
