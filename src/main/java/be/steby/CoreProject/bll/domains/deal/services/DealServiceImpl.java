package be.steby.CoreProject.bll.domains.deal.services;

import be.steby.CoreProject.bll.domains.deal.events.DealCreatedEvent;
import be.steby.CoreProject.bll.domains.deal.events.DealLostEvent;
import be.steby.CoreProject.bll.domains.deal.events.DealReassignedEvent;
import be.steby.CoreProject.bll.domains.deal.events.DealStageChangedEvent;
import be.steby.CoreProject.bll.domains.deal.events.DealUpdatedEvent;
import be.steby.CoreProject.bll.domains.deal.events.DealWonEvent;
import be.steby.CoreProject.bll.domains.deal.exceptions.DealAlreadyClosedException;
import be.steby.CoreProject.bll.domains.deal.exceptions.DealAssignNotAuthorizedException;
import be.steby.CoreProject.bll.domains.deal.exceptions.DealNotFoundException;
import be.steby.CoreProject.bll.domains.deal.exceptions.DealStageNotInPipelineException;
import be.steby.CoreProject.bll.domains.deal.models.DealCreateRequest;
import be.steby.CoreProject.bll.domains.deal.models.DealFilterRequest;
import be.steby.CoreProject.bll.domains.deal.models.DealReassignRequest;
import be.steby.CoreProject.bll.domains.deal.models.DealUpdateRequest;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dal.repositories.crm.PipelineRepository;
import be.steby.CoreProject.dal.repositories.crm.PipelineStepRepository;
import be.steby.CoreProject.dal.specifications.crm.DealSpecification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.entities.crm.Pipeline;
import be.steby.CoreProject.dl.entities.crm.PipelineStep;
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
    private final PipelineRepository pipelineRepository;
    private final PipelineStepRepository pipelineStepRepository;
    private final ContactRepository contactRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public Deal getById(Long id) {
        return dealRepository.findById(id)
                .orElseThrow(() -> DealNotFoundException.byId(id));
    }

    @Override
    public Deal getByPublicId(String publicId) {
        return dealRepository.findByPublicId(publicId)
                .orElseThrow(() -> DealNotFoundException.byPublicId(publicId));
    }

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

        Specification<Deal> spec = Specification.allOf(
                DealSpecification.titleContains(filter.keyword()),
                DealSpecification.hasStatus(filter.status()),
                DealSpecification.inPipeline(pipelineId),
                DealSpecification.atStage(stageId),
                DealSpecification.assignedTo(assignedToId),
                DealSpecification.forContact(contactId),
                DealSpecification.forOrganisation(organisationId),
                DealSpecification.amountBetween(filter.amountMin(), filter.amountMax()),
                DealSpecification.expectedCloseBetween(filter.expectedCloseFrom(), filter.expectedCloseTo())
        );

        return dealRepository.findAll(spec, pageable);
    }

    @Override
    public List<Deal> findByContact(String contactPublicId) {
        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));
        return dealRepository.findByContactIdOrderByCreatedAtDesc(contact.getId());
    }

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
                .contact(contact)
                .organisation(organisation)
                .assignedTo(assignedTo)
                .notes(request.notes())
                .build();

        deal.setPublicId(UUID.randomUUID().toString());

        Deal saved = dealRepository.save(deal);
        log.info("Deal created — publicId: {}, by: {}", saved.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new DealCreatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public Deal update(String publicId, DealUpdateRequest request, User actor) {
        log.debug("Updating deal — publicId: {}, by: {}", publicId, actor.getUsername());

        Deal deal = getByPublicId(publicId);

        if (deal.isClosed()) {
            throw DealAlreadyClosedException.forDeal(publicId);
        }

        if (request.title()             != null) deal.setTitle(request.title());
        if (request.amount()            != null) deal.setAmount(request.amount());
        if (request.currency()          != null) deal.setCurrency(request.currency());
        if (request.expectedCloseDate() != null) deal.setExpectedCloseDate(request.expectedCloseDate());
        if (request.notes()             != null) deal.setNotes(request.notes());

        Deal saved = dealRepository.save(deal);
        log.info("Deal updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new DealUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Stage & lifecycle
    // =========================================================================

    @Override
    @Transactional
    public Deal moveToStage(String publicId, String stagePublicId, User actor) {
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

        if (newStep.isWon()) {
            deal.setStatus(DealStatus.WON);
            deal.setClosedAt(Instant.now());
            Deal saved = dealRepository.save(deal);
            log.info("Deal {} won — stage: {}, by: {}", publicId, stagePublicId, actor.getUsername());
            var device = deviceService.detectAndRegisterDevice(actor);
            eventPublisher.publishEvent(new DealWonEvent(saved, actor, device));
            eventPublisher.publishEvent(new DealStageChangedEvent(saved, previousStep, newStep, actor, device));
            return saved;
        }

        if (newStep.isLost()) {
            deal.setStatus(DealStatus.LOST);
            deal.setClosedAt(Instant.now());
            Deal saved = dealRepository.save(deal);
            log.info("Deal {} lost — stage: {}, by: {}", publicId, stagePublicId, actor.getUsername());
            var device = deviceService.detectAndRegisterDevice(actor);
            eventPublisher.publishEvent(new DealLostEvent(saved, actor, device));
            eventPublisher.publishEvent(new DealStageChangedEvent(saved, previousStep, newStep, actor, device));
            return saved;
        }

        Deal saved = dealRepository.save(deal);
        log.info("Deal {} moved to stage {}, by: {}", publicId, stagePublicId, actor.getUsername());
        eventPublisher.publishEvent(new DealStageChangedEvent(
                saved, previousStep, newStep, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Assignment
    // =========================================================================

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
