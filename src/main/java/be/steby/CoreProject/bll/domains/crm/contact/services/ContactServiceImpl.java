package be.steby.CoreProject.bll.domains.crm.contact.services;

import be.steby.CoreProject.bll.common.models.changelog.FieldChange;
import be.steby.CoreProject.bll.domains.crm.contact.events.ContactAssignedEvent;
import be.steby.CoreProject.bll.domains.crm.contact.events.ContactCreatedEvent;
import be.steby.CoreProject.bll.domains.crm.contact.events.ContactCreatedFromLeadEvent;
import be.steby.CoreProject.bll.domains.crm.contact.events.ContactMergedEvent;
import be.steby.CoreProject.bll.domains.crm.contact.events.ContactStatusChangedEvent;
import be.steby.CoreProject.bll.domains.crm.contact.events.ContactUpdatedEvent;
import be.steby.CoreProject.bll.domains.crm.contact.exceptions.*;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.bll.domains.crm.contact.models.ContactAssignRequest;
import be.steby.CoreProject.bll.domains.crm.contact.models.ContactCreateRequest;
import be.steby.CoreProject.bll.domains.crm.contact.models.ContactFilterRequest;
import be.steby.CoreProject.bll.domains.crm.contact.models.ContactMergeRequest;
import be.steby.CoreProject.bll.domains.crm.contact.models.ContactUpdateRequest;
import be.steby.CoreProject.bll.domains.crm.organisation.models.OrganisationCreateRequest;
import be.steby.CoreProject.bll.domains.crm.organisation.services.OrganisationService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.CommercialActionRepository;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealContactRoleRepository;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dal.repositories.crm.TagRepository;
import be.steby.CoreProject.dal.specifications.crm.ContactSpecification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.DealContactRole;
import be.steby.CoreProject.dl.entities.crm.Interaction;
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

import java.util.ArrayList;
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
 * <p>Flow 1 (automatic) — {@link #createFromLead} is triggered by
 * {@code LeadConversionListener} upon receiving a {@code LeadConvertedEvent}.
 * The actor and device are forwarded from the originating lead conversion action
 * so that the resulting {@code ContactCreatedFromLeadEvent} carries full audit context.
 * If a contact with the same email already exists, it is linked to the lead
 * and returned without creating a duplicate.</p>
 *
 * <p>Flow 2 (manual) — {@link #create(ContactCreateRequest, User)} is called
 * directly by the commercial team to encode a contact from any source.
 * The device is detected at call time via {@link DeviceService}.</p>
 *
 * <h3>Name handling from lead</h3>
 * <p>When creating a contact from a lead, the visitor's name fields ({@code firstName},
 * {@code lastName}) are mapped directly from the lead. The commercial team
 * can complete or correct the record manually via {@link #update}.</p>
 *
 * <h3>Status transition guard</h3>
 * <p>Valid transitions are enforced by {@link #ALLOWED_TRANSITIONS}.
 * Any attempt outside this map throws {@link ContactStatusTransitionException}.</p>
 *
 * <h3>Organisation resolution</h3>
 * <p>During lead conversion, the organisation is resolved by
 * {@link #resolveOrganisationForConversion}: first by public UUID, then by name
 * (find-or-create). For manual creation, only public UUID resolution is supported.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final OrganisationRepository organisationRepository;
    private final OrganisationService organisationService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final InteractionRepository interactionRepository;
    private final CommercialActionRepository commercialActionRepository;
    private final DealContactRoleRepository dealContactRoleRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;
    private final ContactChangeLogService contactChangeLogService;

    // =========================================================================
    // Status transition rules
    // =========================================================================

    /**
     * Defines the allowed CRM status transitions for a contact.
     *
     * <pre>
     * NEW       → ENGAGED, INACTIVE, LOST
     * ENGAGED   → QUALIFIED, INACTIVE, LOST
     * QUALIFIED → CLIENT, LOST, INACTIVE
     * CLIENT    → LOST, INACTIVE
     * LOST      → ENGAGED
     * INACTIVE  → ENGAGED
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

    /** {@inheritDoc} */
    @Override
    public Contact getById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> ContactNotFoundException.byId(id));
    }

    /** {@inheritDoc} */
    @Override
    public Contact getByPublicId(String publicId) {
        return contactRepository.findByPublicId(publicId)
                .orElseThrow(() -> ContactNotFoundException.byPublicId(publicId));
    }

    /** {@inheritDoc} */
    @Override
    public Contact getByEmail(String email) {
        return contactRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ContactNotFoundException.byEmail(email));
    }

    /** {@inheritDoc} */
    @Override
    public Contact getByOriginLeadPublicId(String leadPublicId) {
        return contactRepository.findByOriginLead_PublicId(leadPublicId)
                .orElseThrow(() -> ContactNotFoundException.byOriginLeadPublicId(leadPublicId));
    }

    /** {@inheritDoc} */
    @Override
    public Page<Contact> findAll(ContactFilterRequest filter, Pageable pageable) {
        // Mutual exclusion: withoutOrganisation takes precedence over organisationPublicId
        Specification<Contact> organisationSpec;
        if (Boolean.TRUE.equals(filter.withoutOrganisation())) {
            organisationSpec = ContactSpecification.withoutOrganisation();
        } else if (filter.organisationPublicId() != null) {
            Organisation org = organisationRepository.findByPublicId(filter.organisationPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Organisation not found with publicId: " + filter.organisationPublicId()));
            organisationSpec = ContactSpecification.belongsToOrganisation(org.getId());
        } else {
            organisationSpec = null;
        }

        Long assignedToId = null;
        if (filter.assignedToPublicId() != null) {
            assignedToId = userRepository.findByPublicId(filter.assignedToPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + filter.assignedToPublicId()))
                    .getId();
        }

        Long tagId = null;
        if (filter.tagPublicId() != null) {
            tagId = tagRepository.findByPublicId(filter.tagPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tag not found with publicId: " + filter.tagPublicId()))
                    .getId();
        }

        Specification<Contact> spec = Specification.allOf(
                ContactSpecification.nameOrEmailContains(filter.keyword()),
                ContactSpecification.hasStatus(filter.status()),
                ContactSpecification.hasLinkedUser(filter.hasLinkedUser()),
                ContactSpecification.convertedFromLead(filter.convertedFromLead()),
                ContactSpecification.hasTag(tagId),
                organisationSpec,
                ContactSpecification.assignedTo(assignedToId)
        );

        return contactRepository.findAll(spec, pageable);
    }

    /** {@inheritDoc} */
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

    /**
     * Creates a contact from a converted lead.
     *
     * <p>If a contact with the same email already exists, no duplicate is created:
     * the existing contact is linked to the lead via {@code originLead} and returned.</p>
     *
     * <p>The {@code actor} and {@code actorDevice} are forwarded from the
     * {@code LeadConvertedEvent} so the published {@code ContactCreatedFromLeadEvent}
     * carries the full audit trail of who triggered the conversion and from which device.</p>
     *
     * <p>The organisation is resolved via {@link #resolveOrganisationForConversion}:
     * by public UUID first, then by name (find-or-create), or {@code null} for an independent contact.</p>
     */
    @Override
    @Transactional
    public Contact createFromLead(Lead lead, String organisationPublicId, String organisationName, User actor, Device actorDevice, String emailOverride) {
        String email = (emailOverride != null && !emailOverride.isBlank())
                ? emailOverride.toLowerCase().trim()
                : lead.getEmail();

        log.debug("Creating contact from lead — email: {}", email);

        // If a contact with this email already exists, link it and return
        return contactRepository.findByEmailIgnoreCase(email)
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
                    Organisation organisation = resolveOrganisationForConversion(
                            organisationPublicId, organisationName, actor);

                    String publicId = UUID.randomUUID().toString();

                    Contact contact = Contact.builder()
                            .firstName(lead.getFirstName())
                            .lastName(lead.getLastName())
                            .email(email)
                            .phone(lead.getPhone())
                            .organisation(organisation)
                            .status(ContactStatus.NEW)
                            .originLead(lead)
                            .build();

                    contact.setPublicId(publicId);

                    Contact saved = contactRepository.save(contact);
                    log.info("Contact created from lead — publicId: {}, email: {}, org: {}",
                            saved.getPublicId(), saved.getEmail(),
                            organisation != null ? organisation.getName() : "none");

                    eventPublisher.publishEvent(new ContactCreatedFromLeadEvent(
                            saved, lead, actor, actorDevice));
                    return saved;
                });
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Contact update(String publicId, ContactUpdateRequest request, User actor) {
        log.debug("Updating contact — publicId: {}, by: {}", publicId, actor.getUsername());

        Contact contact = getByPublicId(publicId);

        List<FieldChange> changes = new ArrayList<>();

        if (request.email() != null && !request.email().equalsIgnoreCase(contact.getEmail())) {
            if (contactRepository.existsByEmailIgnoreCase(request.email())) {
                throw ContactEmailAlreadyExistsException.forEmail(request.email());
            }
            changes.add(new FieldChange("email", contact.getEmail(), request.email().toLowerCase().trim()));
            contact.setEmail(request.email().toLowerCase().trim());
        }
        if (request.firstName() != null && !request.firstName().trim().equals(contact.getFirstName())) {
            changes.add(new FieldChange("firstName", contact.getFirstName(), request.firstName().trim()));
            contact.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().trim().equals(contact.getLastName())) {
            changes.add(new FieldChange("lastName", contact.getLastName(), request.lastName().trim()));
            contact.setLastName(request.lastName().trim());
        }
        if (request.phone() != null && !request.phone().equals(contact.getPhone())) {
            changes.add(new FieldChange("phone", contact.getPhone(), request.phone()));
            contact.setPhone(request.phone());
        }
        if (request.jobTitle() != null && !request.jobTitle().equals(contact.getJobTitle())) {
            changes.add(new FieldChange("jobTitle", contact.getJobTitle(), request.jobTitle()));
            contact.setJobTitle(request.jobTitle());
        }
        if (request.notes() != null && !request.notes().equals(contact.getNotes())) {
            changes.add(new FieldChange("notes", contact.getNotes(), request.notes()));
            contact.setNotes(request.notes());
        }

        Contact saved = contactRepository.save(contact);
        log.info("Contact updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new ContactUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));

        contactChangeLogService.logChanges(CrmEntityType.CONTACT, saved.getPublicId(), changes, actor);
        return saved;
    }

    /** {@inheritDoc} */
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

        contactChangeLogService.logChange(
                CrmEntityType.CONTACT, saved.getPublicId(),
                "status", current.name(), newStatus.name(), actor);
        return saved;
    }

    // =========================================================================
    // Relations
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Contact linkUser(String contactPublicId, User user) {
        log.debug("Linking user {} to contact {}", user.getPublicId(), contactPublicId);

        Contact contact = getByPublicId(contactPublicId);

        if (contact.hasLinkedUser()) {
            throw ContactAlreadyLinkedToUserException.contactAlreadyLinked(contactPublicId);
        }

        contactRepository.findByLinkedUser(user).ifPresent(c -> {
            throw ContactAlreadyLinkedToUserException.userAlreadyLinked(user.getPublicId());
        });

        contact.setLinkedUser(user);
        Contact saved = contactRepository.save(contact);

        log.info("User {} linked to contact {}", user.getPublicId(), contactPublicId);
        return saved;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Contact assign(String contactPublicId, ContactAssignRequest request, User actor) {
        log.debug("Assigning contact {} to commercial {}, by: {}",
                contactPublicId, request.commercialPublicId(), actor.getUsername());

        Contact contact = getByPublicId(contactPublicId);

        if (!actor.hasAdminPrivileges() && !actor.getPublicId().equals(request.commercialPublicId())) {
            throw new ContactAssignNotAuthorizedException();
        }

        User previousAssignee = contact.getAssignedTo();

        User newAssignee = null;
        if (request.commercialPublicId() != null) {
            newAssignee = userRepository.findByPublicId(request.commercialPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + request.commercialPublicId()));
        }

        contact.setAssignedTo(newAssignee);
        Contact saved = contactRepository.save(contact);

        log.info("Contact {} assigned to {} by {}",
                contactPublicId,
                newAssignee != null ? newAssignee.getPublicId() : "null (unassigned)",
                actor.getUsername());

        eventPublisher.publishEvent(new ContactAssignedEvent(
                saved, newAssignee, previousAssignee, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Contact merge(ContactMergeRequest request, User actor) {
        if (request.sourcePublicId().equals(request.targetPublicId())) {
            throw new ContactValidationException("Source and target contacts must be different");
        }

        log.debug("Merging contact {} into {}, by: {}",
                request.sourcePublicId(), request.targetPublicId(), actor.getUsername());

        Contact source = getByPublicId(request.sourcePublicId());
        Contact target = getByPublicId(request.targetPublicId());

        // Copy non-null fields from source to target where target is blank
        if (target.getPhone()        == null && source.getPhone()        != null) target.setPhone(source.getPhone());
        if (target.getJobTitle()     == null && source.getJobTitle()     != null) target.setJobTitle(source.getJobTitle());
        if (target.getNotes()        == null && source.getNotes()        != null) target.setNotes(source.getNotes());
        if (target.getOrganisation() == null && source.getOrganisation() != null) target.setOrganisation(source.getOrganisation());
        if (target.getOriginLead()   == null && source.getOriginLead()   != null) target.setOriginLead(source.getOriginLead());

        // Reassign interactions from source to target
        List<Interaction> interactions = interactionRepository.findByContactIdOrderByOccurredAtDesc(source.getId());
        interactions.forEach(i -> i.setContact(target));
        interactionRepository.saveAll(interactions);
        log.info("Contact merge — {} interaction(s) reassigned from {} to {}",
                interactions.size(), source.getPublicId(), target.getPublicId());

        // Reassign commercial actions from source to target
        List<CommercialAction> actions = commercialActionRepository.findByContactIdOrderByDueDateAsc(source.getId());
        actions.forEach(a -> a.setContact(target));
        commercialActionRepository.saveAll(actions);
        log.info("Contact merge — {} commercial action(s) reassigned from {} to {}",
                actions.size(), source.getPublicId(), target.getPublicId());

        // Reassign deal contact roles from source to target
        // If target is already on the same deal, drop the source role to avoid the unique constraint
        List<DealContactRole> sourceRoles = dealContactRoleRepository.findByContactId(source.getId());
        for (DealContactRole role : sourceRoles) {
            Long dealId = role.getDeal().getId();
            if (dealContactRoleRepository.existsByDealIdAndContactId(dealId, target.getId())) {
                dealContactRoleRepository.delete(role);
                log.debug("Contact merge — duplicate DealContactRole dropped (deal {}, source {})",
                        dealId, source.getPublicId());
            } else {
                role.setContact(target);
                dealContactRoleRepository.save(role);
                log.debug("Contact merge — DealContactRole reassigned to target {} (deal {})",
                        target.getPublicId(), dealId);
            }
        }
        log.info("Contact merge — {} deal role(s) processed from {} to {}",
                sourceRoles.size(), source.getPublicId(), target.getPublicId());

        // Archive source contact (soft delete)
        source.setStatus(ContactStatus.INACTIVE);
        contactRepository.save(source);

        Contact savedTarget = contactRepository.save(target);

        log.info("Contact merged — source {} archived, target {} updated, by: {}",
                source.getPublicId(), target.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new ContactMergedEvent(
                savedTarget, source, actor, deviceService.detectAndRegisterDevice(actor)));
        return savedTarget;
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
     * Resolves or creates an organisation during lead conversion.
     *
     * <p>Priority:</p>
     * <ol>
     *   <li>{@code organisationPublicId} → resolved directly by UUID</li>
     *   <li>{@code organisationName} → found by name (case-insensitive) or created on the fly</li>
     *   <li>Neither provided → returns {@code null} (independent contact)</li>
     * </ol>
     */
    private Organisation resolveOrganisationForConversion(
            String organisationPublicId, String organisationName, User actor) {

        if (organisationPublicId != null && !organisationPublicId.isBlank()) {
            return resolveOrganisation(organisationPublicId);
        }

        if (organisationName != null && !organisationName.isBlank()) {
            return organisationRepository.findByNameIgnoreCase(organisationName.trim())
                    .orElseGet(() -> {
                        log.info("Organisation '{}' not found — creating on the fly during lead conversion",
                                organisationName);
                        return organisationService.create(
                                new OrganisationCreateRequest(
                                        organisationName.trim(),
                                        null, null, null, null, null, null),
                                actor);
                    });
        }

        return null;
    }
}
