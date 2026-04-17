package be.steby.CoreProject.bll.domains.crm.deal.services;

import be.steby.CoreProject.bll.common.models.changelog.FieldChange;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealCreatedEvent;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealLostEvent;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealReassignedEvent;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealStageChangedEvent;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealUpdatedEvent;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealWonEvent;
import be.steby.CoreProject.bll.domains.crm.deal.exceptions.DealAlreadyClosedException;
import be.steby.CoreProject.bll.domains.crm.deal.exceptions.DealAssignNotAuthorizedException;
import be.steby.CoreProject.bll.domains.crm.deal.exceptions.DealNotFoundException;
import be.steby.CoreProject.bll.domains.crm.deal.exceptions.DealStageNotInPipelineException;
import be.steby.CoreProject.bll.domains.crm.deal.models.DealAddContactRoleRequest;
import be.steby.CoreProject.bll.domains.crm.deal.models.DealCreateRequest;
import be.steby.CoreProject.bll.domains.crm.deal.models.DealFilterRequest;
import be.steby.CoreProject.bll.domains.crm.deal.models.DealReassignRequest;
import be.steby.CoreProject.bll.domains.crm.deal.models.DealUpdateRequest;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealContactRoleRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dal.repositories.crm.PipelineRepository;
import be.steby.CoreProject.dal.repositories.crm.PipelineStepRepository;
import be.steby.CoreProject.dal.repositories.crm.TagRepository;
import be.steby.CoreProject.dal.specifications.crm.DealSpecification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.DealContactRole;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.entities.crm.Pipeline;
import be.steby.CoreProject.dl.entities.crm.PipelineStep;
import be.steby.CoreProject.dl.enums.crm.ContactRole;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link DealService}.
 *
 * <h3>Creation flow</h3>
 * <p>{@link #create(DealCreateRequest, User)} is called directly by the commercial team.
 * The stage must belong to the pipeline — validated before persisting.</p>
 *
 * <h3>Stage transition guard</h3>
 * <p>Closed deals ({@code WON} or {@code LOST}) cannot be updated or have their
 * stage moved. Reassignment is allowed regardless of status.</p>
 *
 * <h3>Terminal stage handling</h3>
 * <p>When a deal enters a terminal stage, the service sets {@code status} and
 * {@code closedAt} automatically and publishes the appropriate domain event
 * ({@code DealWonEvent} or {@code DealLostEvent}) in addition to
 * {@code DealStageChangedEvent}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final DealContactRoleRepository dealContactRoleRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStepRepository pipelineStepRepository;
    private final ContactRepository contactRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;
    private final DealChangeLogService dealChangeLogService;

    // =========================================================================
    // Lookup
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    public Deal getById(Long id) {
        return dealRepository.findById(id)
                .orElseThrow(() -> DealNotFoundException.byId(id));
    }

    /** {@inheritDoc} */
    @Override
    public Deal getByPublicId(String publicId) {
        return dealRepository.findByPublicId(publicId)
                .orElseThrow(() -> DealNotFoundException.byPublicId(publicId));
    }

    /** {@inheritDoc} */
    @Override
    public Page<Deal> findAll(DealFilterRequest filter, Pageable pageable) {

        // overdueOnly takes precedence — skip all other date/status filters
        if (Boolean.TRUE.equals(filter.overdueOnly())) {
            return dealRepository.findAll(DealSpecification.isOverdue(), pageable);
        }

        // Resolve publicIds to internal IDs for specification building
        Long pipelineId = null;
        if (filter.pipelinePublicId() != null) {
            pipelineId = pipelineRepository.findByPublicId(filter.pipelinePublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Pipeline not found with publicId: " + filter.pipelinePublicId()))
                    .getId();
        }

        Long stageId = null;
        if (filter.stagePublicId() != null) {
            stageId = pipelineStepRepository.findByPublicId(filter.stagePublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Pipeline step not found with publicId: " + filter.stagePublicId()))
                    .getId();
        }

        Long assignedToId = null;
        if (filter.assignedToPublicId() != null) {
            assignedToId = userRepository.findByPublicId(filter.assignedToPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + filter.assignedToPublicId()))
                    .getId();
        }

        Long contactId = null;
        if (filter.contactPublicId() != null) {
            contactId = contactRepository.findByPublicId(filter.contactPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Contact not found with publicId: " + filter.contactPublicId()))
                    .getId();
        }

        Long organisationId = null;
        if (filter.organisationPublicId() != null) {
            organisationId = organisationRepository.findByPublicId(filter.organisationPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Organisation not found with publicId: " + filter.organisationPublicId()))
                    .getId();
        }

        Long tagId = null;
        if (filter.tagPublicId() != null) {
            tagId = tagRepository.findByPublicId(filter.tagPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tag not found with publicId: " + filter.tagPublicId()))
                    .getId();
        }

        Specification<Deal> spec = Specification.allOf(
                DealSpecification.titleContains(filter.keyword()),
                DealSpecification.hasStatus(filter.status()),
                DealSpecification.inPipeline(pipelineId),
                DealSpecification.atStage(stageId),
                DealSpecification.assignedTo(assignedToId),
                DealSpecification.forContact(contactId),
                DealSpecification.forOrganisation(organisationId),
                DealSpecification.hasTag(tagId),
                DealSpecification.amountBetween(filter.amountMin(), filter.amountMax()),
                DealSpecification.expectedCloseBetween(filter.expectedCloseFrom(), filter.expectedCloseTo())
        );

        return dealRepository.findAll(spec, pageable);
    }

    /** {@inheritDoc} */
    @Override
    public List<Deal> findByContact(String contactPublicId) {
        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));
        return dealRepository.findByContactIdOrderByCreatedAtDesc(contact.getId());
    }

    /** {@inheritDoc} */
    @Override
    public List<Deal> findByOrganisation(String organisationPublicId) {
        Organisation organisation = organisationRepository.findByPublicId(organisationPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organisation not found with publicId: " + organisationPublicId));
        return dealRepository.findByOrganisationIdOrderByCreatedAtDesc(organisation.getId());
    }

    // =========================================================================
    // Creation & update
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Deal create(DealCreateRequest request, User actor) {
        log.debug("Creating deal — title: '{}', by: {}", request.title(), actor.getUsername());

        Pipeline pipeline = pipelineRepository.findByPublicId(request.pipelinePublicId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pipeline not found with publicId: " + request.pipelinePublicId()));

        PipelineStep step = pipelineStepRepository.findByPublicId(request.stagePublicId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pipeline step not found with publicId: " + request.stagePublicId()));

        // Validate that stage belongs to the pipeline
        if (!step.getPipeline().getId().equals(pipeline.getId())) {
            throw DealStageNotInPipelineException.forStageAndPipeline(
                    request.stagePublicId(), request.pipelinePublicId());
        }

        Contact contact = contactRepository.findByPublicId(request.contactPublicId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + request.contactPublicId()));

        Organisation organisation = resolveOrganisation(request.organisationPublicId());

        User assignedTo = userRepository.findByPublicId(request.assignedToPublicId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with publicId: " + request.assignedToPublicId()));

        Deal deal = Deal.builder()
                .title(request.title())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .status(DealStatus.OPEN)
                .expectedCloseDate(request.expectedCloseDate())
                .pipeline(pipeline)
                .pipelineStep(step)
                .organisation(organisation)
                .assignedTo(assignedTo)
                .notes(request.notes())
                .build();

        deal.setPublicId(UUID.randomUUID().toString());

        Deal saved = dealRepository.save(deal);

        DealContactRole primaryRole = DealContactRole.builder()
                .deal(saved)
                .contact(contact)
                .role(ContactRole.OTHER)
                .primary(true)
                .build();
        dealContactRoleRepository.save(primaryRole);

        log.info("Deal created — publicId: {}, primaryContact: {}, by: {}",
                saved.getPublicId(), contact.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new DealCreatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Deal update(String publicId, DealUpdateRequest request, User actor) {
        log.debug("Updating deal — publicId: {}, by: {}", publicId, actor.getUsername());

        Deal deal = getByPublicId(publicId);

        if (deal.isClosed()) {
            throw DealAlreadyClosedException.forDeal(publicId);
        }

        List<FieldChange> changes = new ArrayList<>();

        if (request.title() != null && !request.title().equals(deal.getTitle())) {
            changes.add(new FieldChange("title", deal.getTitle(), request.title()));
            deal.setTitle(request.title());
        }
        if (request.amount() != null && !request.amount().equals(deal.getAmount())) {
            changes.add(new FieldChange("amount",
                    deal.getAmount() != null ? deal.getAmount().toPlainString() : null,
                    request.amount().toPlainString()));
            deal.setAmount(request.amount());
        }
        if (request.currency() != null && !request.currency().equals(deal.getCurrency())) {
            changes.add(new FieldChange("currency", deal.getCurrency(), request.currency()));
            deal.setCurrency(request.currency());
        }
        if (request.expectedCloseDate() != null && !request.expectedCloseDate().equals(deal.getExpectedCloseDate())) {
            changes.add(new FieldChange("expectedCloseDate",
                    deal.getExpectedCloseDate() != null ? deal.getExpectedCloseDate().toString() : null,
                    request.expectedCloseDate().toString()));
            deal.setExpectedCloseDate(request.expectedCloseDate());
        }
        if (request.notes() != null && !request.notes().equals(deal.getNotes())) {
            changes.add(new FieldChange("notes", deal.getNotes(), request.notes()));
            deal.setNotes(request.notes());
        }

        Deal saved = dealRepository.save(deal);
        log.info("Deal updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new DealUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));

        dealChangeLogService.logChanges(CrmEntityType.DEAL, saved.getPublicId(), changes, actor);
        return saved;
    }

    // =========================================================================
    // Stage & lifecycle
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Deal moveToStage(String publicId, String stagePublicId, String lostReason, User actor) {
        log.debug("Moving deal {} to stage {}, by: {}", publicId, stagePublicId, actor.getUsername());

        Deal deal = getByPublicId(publicId);

        if (deal.isClosed()) {
            throw DealAlreadyClosedException.forDeal(publicId);
        }

        PipelineStep newStep = pipelineStepRepository.findByPublicId(stagePublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pipeline step not found with publicId: " + stagePublicId));

        // Validate stage belongs to the deal's pipeline
        if (!newStep.getPipeline().getId().equals(deal.getPipeline().getId())) {
            throw DealStageNotInPipelineException.forStageAndPipeline(
                    stagePublicId, deal.getPipeline().getPublicId());
        }

        PipelineStep previousStep = deal.getPipelineStep();
        deal.setPipelineStep(newStep);

        String previousStageName = previousStep != null ? previousStep.getName() : null;

        if (newStep.isWon()) {
            deal.setStatus(DealStatus.WON);
            deal.setClosedAt(Instant.now());
            Deal saved = dealRepository.save(deal);
            log.info("Deal {} won — stage: {}, by: {}", publicId, stagePublicId, actor.getUsername());
            var device = deviceService.detectAndRegisterDevice(actor);
            eventPublisher.publishEvent(new DealWonEvent(saved, actor, device));
            eventPublisher.publishEvent(new DealStageChangedEvent(saved, previousStep, newStep, actor, device));
            dealChangeLogService.logChange(CrmEntityType.DEAL, saved.getPublicId(),
                    "stage", previousStageName, newStep.getName(), actor);
            return saved;
        }

        if (newStep.isLost()) {
            deal.setStatus(DealStatus.LOST);
            deal.setClosedAt(Instant.now());
            deal.setLostReason(lostReason);
            Deal saved = dealRepository.save(deal);
            log.info("Deal {} lost — stage: {}, reason: '{}', by: {}",
                    publicId, stagePublicId, lostReason, actor.getUsername());
            var device = deviceService.detectAndRegisterDevice(actor);
            eventPublisher.publishEvent(new DealLostEvent(saved, actor, device));
            eventPublisher.publishEvent(new DealStageChangedEvent(saved, previousStep, newStep, actor, device));
            dealChangeLogService.logChange(CrmEntityType.DEAL, saved.getPublicId(),
                    "stage", previousStageName, newStep.getName(), actor);
            if (lostReason != null) {
                dealChangeLogService.logChange(CrmEntityType.DEAL, saved.getPublicId(),
                        "lostReason", null, lostReason, actor);
            }
            return saved;
        }

        Deal saved = dealRepository.save(deal);
        log.info("Deal {} moved to stage {}, by: {}", publicId, stagePublicId, actor.getUsername());
        eventPublisher.publishEvent(new DealStageChangedEvent(
                saved, previousStep, newStep, actor, deviceService.detectAndRegisterDevice(actor)));
        dealChangeLogService.logChange(CrmEntityType.DEAL, saved.getPublicId(),
                "stage", previousStageName, newStep.getName(), actor);
        return saved;
    }

    // =========================================================================
    // Contact roles
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DealContactRole addContactRole(String dealPublicId, DealAddContactRoleRequest request, User actor) {
        Deal deal = getByPublicId(dealPublicId);

        Contact contact = contactRepository.findByPublicId(request.contactPublicId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + request.contactPublicId()));

        if (dealContactRoleRepository.existsByDealIdAndContactId(deal.getId(), contact.getId())) {
            throw new IllegalArgumentException(
                    "Contact " + request.contactPublicId() + " is already on deal " + dealPublicId);
        }

        boolean isFirst = dealContactRoleRepository.findByDealId(deal.getId()).isEmpty();

        DealContactRole role = DealContactRole.builder()
                .deal(deal)
                .contact(contact)
                .role(request.role() != null ? request.role() : ContactRole.OTHER)
                .primary(isFirst)
                .build();

        DealContactRole saved = dealContactRoleRepository.save(role);
        log.info("Contact {} added to deal {} (role: {}, primary: {}), by: {}",
                contact.getPublicId(), dealPublicId, saved.getRole(), saved.isPrimary(), actor.getUsername());
        return saved;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void removeContactRole(String dealPublicId, String contactPublicId, User actor) {
        Deal deal = getByPublicId(dealPublicId);

        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));

        DealContactRole roleToRemove = dealContactRoleRepository
                .findByDealIdAndContactId(deal.getId(), contact.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact " + contactPublicId + " is not on deal " + dealPublicId));

        List<DealContactRole> allRoles = dealContactRoleRepository.findByDealId(deal.getId());

        if (allRoles.size() == 1) {
            throw new IllegalStateException(
                    "Cannot remove the last contact from a deal. Add another contact first.");
        }

        if (roleToRemove.isPrimary()) {
            throw new IllegalStateException(
                    "Cannot remove the primary contact. Promote another contact first via setPrimaryContact.");
        }

        dealContactRoleRepository.delete(roleToRemove);
        log.info("Contact {} removed from deal {}, by: {}", contactPublicId, dealPublicId, actor.getUsername());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DealContactRole updateContactRole(String dealPublicId, String contactPublicId, ContactRole role, User actor) {
        Deal deal = getByPublicId(dealPublicId);

        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));

        DealContactRole existing = dealContactRoleRepository
                .findByDealIdAndContactId(deal.getId(), contact.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact " + contactPublicId + " is not on deal " + dealPublicId));

        existing.setRole(role);
        DealContactRole saved = dealContactRoleRepository.save(existing);
        log.info("Contact {} role on deal {} updated to {}, by: {}",
                contactPublicId, dealPublicId, role, actor.getUsername());
        return saved;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DealContactRole setPrimaryContact(String dealPublicId, String contactPublicId, User actor) {
        Deal deal = getByPublicId(dealPublicId);

        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));

        DealContactRole newPrimary = dealContactRoleRepository
                .findByDealIdAndContactId(deal.getId(), contact.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact " + contactPublicId + " is not on deal " + dealPublicId));

        // Unset current primary
        dealContactRoleRepository.findPrimaryByDealId(deal.getId()).ifPresent(current -> {
            if (!current.getId().equals(newPrimary.getId())) {
                current.setPrimary(false);
                dealContactRoleRepository.save(current);
            }
        });

        newPrimary.setPrimary(true);
        DealContactRole saved = dealContactRoleRepository.save(newPrimary);
        log.info("Contact {} set as primary on deal {}, by: {}",
                contactPublicId, dealPublicId, actor.getUsername());
        return saved;
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Deal reassign(String publicId, DealReassignRequest request, User actor) {
        log.debug("Reassigning deal {} to {}, by: {}",
                publicId, request.assignedToPublicId(), actor.getUsername());

        Deal deal = getByPublicId(publicId);

        if (!actor.hasAdminPrivileges() && !actor.getPublicId().equals(request.assignedToPublicId())) {
            throw new DealAssignNotAuthorizedException();
        }

        User previousAssignee = deal.getAssignedTo();

        User newAssignee = null;
        if (request.assignedToPublicId() != null) {
            newAssignee = userRepository.findByPublicId(request.assignedToPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + request.assignedToPublicId()));
        }

        deal.setAssignedTo(newAssignee);
        Deal saved = dealRepository.save(deal);

        log.info("Deal {} reassigned to {}, by: {}",
                publicId,
                newAssignee != null ? newAssignee.getPublicId() : "null (unassigned)",
                actor.getUsername());

        eventPublisher.publishEvent(new DealReassignedEvent(
                saved, newAssignee, previousAssignee, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves an organisation entity from its public UUID.
     * Returns {@code null} if {@code publicId} is null (deal without organisation).
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
}
