package be.steby.CoreProject.bll.domains.contact.services;

import be.steby.CoreProject.bll.domains.contact.events.ContactCreatedEvent;
import be.steby.CoreProject.bll.domains.contact.events.ContactCreatedFromLeadEvent;
import be.steby.CoreProject.bll.domains.contact.events.ContactStatusChangedEvent;
import be.steby.CoreProject.bll.domains.contact.events.ContactUpdatedEvent;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.contact.exceptions.*;
import be.steby.CoreProject.bll.domains.contact.models.ContactCreateRequest;
import be.steby.CoreProject.bll.domains.contact.models.ContactFilterRequest;
import be.steby.CoreProject.bll.domains.contact.models.ContactUpdateRequest;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dal.specifications.crm.ContactSpecification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of {@link ContactService}.
 *
 * <h3>Creation flows</h3>
 * <p>Flow 1 (automatic) — {@link #createFromLead(Lead)} is triggered by
 * {@code ContactCreationListener} upon receiving a {@code LeadSubmittedEvent}.
 * If a contact with the same email already exists, it is linked to the lead
 * and returned without creating a duplicate.</p>
 *
 * <p>Flow 2 (manual) — {@link #create(ContactCreateRequest, User)} is called
 * directly by the commercial team to encode a contact from any source.</p>
 *
 * <h3>Status transition guard</h3>
 * <p>Valid transitions are defined in {@link #ALLOWED_TRANSITIONS}.
 * Any attempt outside this map throws {@link ContactStatusTransitionException}.</p>
 *
 * <h3>Note on pending additions</h3>
 * <ul>
 *   <li>{@code ContactRepository.findByLinkedUser(User)} — needed by {@link #linkUser}
 *       to guard against a user already linked to another contact</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final OrganisationRepository organisationRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Status transition rules
    // =========================================================================

    /**
     * Defines the allowed CRM status transitions for a contact.
     *
     * <pre>
     * NEW      → ENGAGED, INACTIVE, LOST
     * ENGAGED  → QUALIFIED, INACTIVE, LOST
     * QUALIFIED→ CLIENT, LOST, INACTIVE
     * CLIENT   → LOST, INACTIVE
     * LOST     → ENGAGED
     * INACTIVE → ENGAGED
     * </pre>
     */
    private static final Map<ContactStatus, Set<ContactStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(ContactStatus.class);
        ALLOWED_TRANSITIONS.put(ContactStatus.NEW,
                EnumSet.of(ContactStatus.ENGAGED, ContactStatus.INACTIVE, ContactStatus.LOST));
        ALLOWED_TRANSITIONS.put(ContactStatus.ENGAGED,
                EnumSet.of(ContactStatus.QUALIFIED, ContactStatus.INACTIVE, ContactStatus.LOST));
        ALLOWED_TRANSITIONS.put(ContactStatus.QUALIFIED,
                EnumSet.of(ContactStatus.CLIENT, ContactStatus.LOST, ContactStatus.INACTIVE));
        ALLOWED_TRANSITIONS.put(ContactStatus.CLIENT,
                EnumSet.of(ContactStatus.LOST, ContactStatus.INACTIVE));
        ALLOWED_TRANSITIONS.put(ContactStatus.LOST,
                EnumSet.of(ContactStatus.ENGAGED));
        ALLOWED_TRANSITIONS.put(ContactStatus.INACTIVE,
                EnumSet.of(ContactStatus.ENGAGED));
    }

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public Contact getById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(
                        "Contact not found with id: " + id));
    }

    @Override
    public Contact getByPublicId(String publicId) {
        return contactRepository.findByPublicId(publicId)
                .orElseThrow(() -> ContactNotFoundException.byPublicId(publicId));
    }

    @Override
    public Contact getByEmail(String email) {
        return contactRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ContactNotFoundException.byEmail(email));
    }

    @Override
    public Page<Contact> findAll(ContactFilterRequest filter, Pageable pageable) {
        Specification<Contact> spec = ContactSpecification.nameOrEmailContains(filter.keyword())
                .and(ContactSpecification.hasStatus(filter.status()))
                .and(ContactSpecification.hasLinkedUser(filter.hasLinkedUser()))
                .and(ContactSpecification.convertedFromLead(filter.convertedFromLead()));

        // Mutual exclusion: withoutOrganisation takes precedence over organisationPublicId
        if (Boolean.TRUE.equals(filter.withoutOrganisation())) {
            spec = spec.and(ContactSpecification.withoutOrganisation());
        } else if (filter.organisationPublicId() != null) {
            Organisation org = organisationRepository.findByPublicId(filter.organisationPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Organisation not found with publicId: " + filter.organisationPublicId()));
            spec = spec.and(ContactSpecification.belongsToOrganisation(org.getId()));
        }

        // TODO: ContactSpecification.assignedTo(id) not yet implemented
        // Requires adding assignedTo field to Contact entity + spec predicate

        return contactRepository.findAll(spec, pageable);
    }

    @Override
    public List<Contact> findByOrganisation(String organisationPublicId) {
        Organisation org = organisationRepository.findByPublicId(organisationPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organisation not found with publicId: " + organisationPublicId));
        return contactRepository.findByOrganisationId(org.getId());
    }

    // =========================================================================
    // Creation
    // =========================================================================

    @Override
    @Transactional
    public Contact createFromLead(Lead lead) {
        log.debug("Creating contact from lead — email: {}", lead.getEmail());

        // If a contact with this email already exists, link it and return
        return contactRepository.findByEmailIgnoreCase(lead.getEmail())
                .map(existing -> {
                    log.info("Contact with email {} already exists (publicId: {}), linking to lead",
                            lead.getEmail(), existing.getPublicId());
                    if (existing.getOriginLead() == null) {
                        existing.setOriginLead(lead);
                        contactRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    String publicId = UUID.randomUUID().toString();
                    Contact contact = Contact.builder()
                            .firstName(lead.getFirstname())
                            .lastName(lead.getLastname())
                            .email(lead.getEmail().toLowerCase().trim())
                            .phone(lead.getPhone())
                            .status(ContactStatus.NEW)
                            .originLead(lead)
                            .build();

                    contact.setPublicId(publicId);

                    Contact saved = contactRepository.save(contact);
                    log.info("Contact created from lead — publicId: {}, email: {}",
                            saved.getPublicId(), saved.getEmail());

                    eventPublisher.publishEvent(new ContactCreatedFromLeadEvent(
                            saved, lead, null, null));
                    return saved;
                });
    }

    @Override
    @Transactional
    public Contact create(ContactCreateRequest request, User actor) {
        log.debug("Creating contact manually — email: {}, by: {}",
                request.email(), actor.getUsername());

        if (contactRepository.existsByEmailIgnoreCase(request.email())) {
            throw ContactEmailAlreadyExistsException.forEmail(request.email());
        }

        Organisation organisation = resolveOrganisation(request.organisationPublicId());

        String publicId = UUID.randomUUID().toString();

        Contact contact = Contact.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(request.email().toLowerCase().trim())
                .phone(request.phone())
                .jobTitle(request.jobTitle())
                .notes(request.notes())
                .organisation(organisation)
                .status(ContactStatus.NEW)
                .build();

        contact.setPublicId(publicId);

        Contact saved = contactRepository.save(contact);
        log.info("Contact created manually — publicId: {}, by: {}",
                saved.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new ContactCreatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Update
    // =========================================================================

    @Override
    @Transactional
    public Contact update(String publicId, ContactUpdateRequest request, User actor) {
        log.debug("Updating contact — publicId: {}, by: {}", publicId, actor.getUsername());

        Contact contact = getByPublicId(publicId);

        if (request.email() != null && !request.email().equalsIgnoreCase(contact.getEmail())) {
            if (contactRepository.existsByEmailIgnoreCase(request.email())) {
                throw ContactEmailAlreadyExistsException.forEmail(request.email());
            }
            contact.setEmail(request.email().toLowerCase().trim());
        }
        if (request.firstName() != null) contact.setFirstName(request.firstName().trim());
        if (request.lastName()  != null) contact.setLastName(request.lastName().trim());
        if (request.phone()     != null) contact.setPhone(request.phone());
        if (request.jobTitle()  != null) contact.setJobTitle(request.jobTitle());
        if (request.notes()     != null) contact.setNotes(request.notes());

        Contact saved = contactRepository.save(contact);
        log.info("Contact updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new ContactUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public Contact updateStatus(String publicId, ContactStatus newStatus, User actor) {
        log.debug("Updating contact status — publicId: {}, newStatus: {}, by: {}",
                publicId, newStatus, actor.getUsername());

        Contact contact = getByPublicId(publicId);
        ContactStatus current = contact.getStatus();

        guardStatusTransition(contact, newStatus);

        contact.setStatus(newStatus);
        Contact saved = contactRepository.save(contact);

        log.info("Contact status changed — publicId: {}, {} → {}, by: {}",
                publicId, current, newStatus, actor.getUsername());

        eventPublisher.publishEvent(new ContactStatusChangedEvent(
                saved, current, newStatus, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Relations
    // =========================================================================

    @Override
    @Transactional
    public Contact linkUser(String contactPublicId, User user) {
        log.debug("Linking user {} to contact {}", user.getPublicId(), contactPublicId);

        Contact contact = getByPublicId(contactPublicId);

        if (contact.hasLinkedUser()) {
            throw ContactAlreadyLinkedToUserException.contactAlreadyLinked(contactPublicId);
        }

        // TODO: requires ContactRepository.findByLinkedUser(User) to be added
        // contactRepository.findByLinkedUser(user).ifPresent(c -> {
        //     throw ContactAlreadyLinkedToUserException.userAlreadyLinked(user.getPublicId());
        // });

        contact.setLinkedUser(user);
        Contact saved = contactRepository.save(contact);

        log.info("User {} linked to contact {}", user.getPublicId(), contactPublicId);
        return saved;
    }

    @Override
    @Transactional
    public Contact linkOrganisation(String contactPublicId, String organisationPublicId, User actor) {
        log.debug("Linking organisation {} to contact {}, by: {}",
                organisationPublicId, contactPublicId, actor.getUsername());

        Contact contact = getByPublicId(contactPublicId);
        Organisation organisation = resolveOrganisation(organisationPublicId);

        contact.setOrganisation(organisation);
        Contact saved = contactRepository.save(contact);

        log.info("Organisation {} linked to contact {} by {}",
                organisationPublicId != null ? organisationPublicId : "null (unlinked)",
                contactPublicId, actor.getUsername());
        return saved;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Validates that the requested status transition is allowed.
     *
     * @param contact   the contact to transition
     * @param newStatus the requested target status
     * @throws ContactStatusTransitionException if the transition is not in {@link #ALLOWED_TRANSITIONS}
     */
    private void guardStatusTransition(Contact contact, ContactStatus newStatus) {
        Set<ContactStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(contact.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw ContactStatusTransitionException.invalidTransition(
                    contact.getPublicId(), contact.getStatus(), newStatus);
        }
    }

    /**
     * Resolves an organisation entity from its public UUID.
     * Returns {@code null} if {@code publicId} is null (independent contact).
     *
     * @param organisationPublicId the public UUID of the organisation, or {@code null}
     * @return the organisation entity, or {@code null}
     * @throws IllegalArgumentException if the publicId is provided but yields no result
     */
    private Organisation resolveOrganisation(String organisationPublicId) {
        if (organisationPublicId == null) return null;
        return organisationRepository.findByPublicId(organisationPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organisation not found with publicId: " + organisationPublicId));
    }

    /**
     * Splits a raw name string into {@code [firstName, lastName]}.
     *
     * <p>Splits on the first space. If no space is found, the full name
     * is placed in {@code firstName} and {@code lastName} is left empty.
     * If the name is null or blank, both parts are empty strings.</p>
     *
     * @param name the raw name from the lead (e.g. "Thomas Dupont")
     * @return a two-element array {@code [firstName, lastName]}
     */
    private String[] splitName(String name) {
        if (name == null || name.isBlank()) return new String[]{"", ""};
        String trimmed = name.trim();
        int idx = trimmed.indexOf(' ');
        if (idx == -1) return new String[]{trimmed, ""};
        return new String[]{trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim()};
    }
}