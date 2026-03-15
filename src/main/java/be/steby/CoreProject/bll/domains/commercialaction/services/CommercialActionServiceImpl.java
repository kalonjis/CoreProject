package be.steby.CoreProject.bll.domains.commercialaction.services;

import be.steby.CoreProject.bll.domains.commercialaction.events.CommercialActionCancelledEvent;
import be.steby.CoreProject.bll.domains.commercialaction.events.CommercialActionCompletedEvent;
import be.steby.CoreProject.bll.domains.commercialaction.events.CommercialActionCreatedEvent;
import be.steby.CoreProject.bll.domains.commercialaction.events.CommercialActionReassignedEvent;
import be.steby.CoreProject.bll.domains.commercialaction.events.CommercialActionUpdatedEvent;
import be.steby.CoreProject.bll.domains.commercialaction.exceptions.CommercialActionAlreadyTerminatedException;
import be.steby.CoreProject.bll.domains.commercialaction.exceptions.CommercialActionNotFoundException;
import be.steby.CoreProject.bll.domains.commercialaction.exceptions.CommercialActionValidationException;
import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionCreateRequest;
import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionFilterRequest;
import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionUpdateRequest;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.CommercialActionRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link CommercialActionService}.
 *
 * <h3>Creation flow</h3>
 * <p>{@link #create(CommercialActionCreateRequest, User)} validates that at least one of
 * {@code dealPublicId} or {@code contactPublicId} is set, resolves all referenced entities,
 * builds the {@link CommercialAction} with a UUID publicId, persists it, and publishes
 * {@code CommercialActionCreatedEvent}.</p>
 *
 * <h3>Terminal state guard</h3>
 * <p>Actions in {@code DONE} or {@code CANCELLED} status cannot be updated, completed,
 * or cancelled again. The guard throws {@link CommercialActionAlreadyTerminatedException}
 * on any such attempt.</p>
 *
 * <h3>Reassignment</h3>
 * <p>Reassignment is supported both as a dedicated operation and inline within update.
 * In both cases, the previous assignee is captured before the change and a
 * {@code CommercialActionReassignedEvent} is published.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommercialActionServiceImpl implements CommercialActionService {

    private final CommercialActionRepository commercialActionRepository;
    private final DealRepository dealRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public CommercialAction getByPublicId(String publicId) {
        return commercialActionRepository.findByPublicId(publicId)
                .orElseThrow(() -> CommercialActionNotFoundException.byPublicId(publicId));
    }

    @Override
    public List<CommercialAction> findByDeal(String dealPublicId) {
        Deal deal = dealRepository.findByPublicId(dealPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deal not found with publicId: " + dealPublicId));
        return commercialActionRepository.findByDealIdOrderByDueDateAsc(deal.getId());
    }

    @Override
    public List<CommercialAction> findByContact(String contactPublicId) {
        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));
        return commercialActionRepository.findByContactIdOrderByDueDateAsc(contact.getId());
    }

    @Override
    public List<CommercialAction> findByAssignedTo(String assignedToPublicId, CommercialActionStatus status) {
        User user = userRepository.findByPublicId(assignedToPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with publicId: " + assignedToPublicId));
        CommercialActionStatus effectiveStatus = status != null ? status : CommercialActionStatus.PENDING;
        return commercialActionRepository.findByAssignedToIdAndStatusOrderByDueDateAsc(
                user.getId(), effectiveStatus);
    }

    // =========================================================================
    // Write
    // =========================================================================

    @Override
    @Transactional
    public CommercialAction create(CommercialActionCreateRequest request, User actor) {
        log.debug("Creating commercial action — title: '{}', by: {}", request.title(), actor.getUsername());

        // Guard: at least one of deal/contact must be provided
        if (request.dealPublicId() == null && request.contactPublicId() == null) {
            throw CommercialActionValidationException.neitherDealNorContact();
        }

        // Guard: assignedTo is required
        if (request.assignedToPublicId() == null) {
            throw CommercialActionValidationException.missingAssignee();
        }

        // Resolve optional deal reference
        Deal deal = resolveDeal(request.dealPublicId());

        // Resolve optional contact reference
        Contact contact = resolveContact(request.contactPublicId());

        // Resolve required assignee
        User assignedTo = userRepository.findByPublicId(request.assignedToPublicId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with publicId: " + request.assignedToPublicId()));

        CommercialAction action = CommercialAction.builder()
                .title(request.title())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : CommercialActionPriority.MEDIUM)
                .status(CommercialActionStatus.PENDING)
                .dueDate(request.dueDate())
                .assignedTo(assignedTo)
                .deal(deal)
                .contact(contact)
                .build();

        action.setPublicId(UUID.randomUUID().toString());

        CommercialAction saved = commercialActionRepository.save(action);
        log.info("Commercial action created — publicId: {}, by: {}", saved.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new CommercialActionCreatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public CommercialAction update(String publicId, CommercialActionUpdateRequest request, User actor) {
        log.debug("Updating commercial action — publicId: {}, by: {}", publicId, actor.getUsername());

        CommercialAction action = getByPublicId(publicId);

        if (action.getStatus() != CommercialActionStatus.PENDING) {
            throw CommercialActionAlreadyTerminatedException.forAction(publicId, action.getStatus());
        }

        if (request.title()       != null) action.setTitle(request.title());
        if (request.description() != null) action.setDescription(request.description());
        if (request.priority()    != null) action.setPriority(request.priority());
        if (request.dueDate()     != null) action.setDueDate(request.dueDate());

        // Inline reassignment
        if (request.assignedToPublicId() != null) {
            User previousAssignee = action.getAssignedTo();
            User newAssignee = userRepository.findByPublicId(request.assignedToPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + request.assignedToPublicId()));
            action.setAssignedTo(newAssignee);

            CommercialAction saved = commercialActionRepository.save(action);
            log.info("Commercial action updated (with reassignment) — publicId: {}, by: {}",
                    publicId, actor.getUsername());

            var device = deviceService.detectAndRegisterDevice(actor);
            eventPublisher.publishEvent(new CommercialActionReassignedEvent(
                    saved, newAssignee, previousAssignee, actor, device));
            eventPublisher.publishEvent(new CommercialActionUpdatedEvent(
                    saved, actor, device));
            return saved;
        }

        CommercialAction saved = commercialActionRepository.save(action);
        log.info("Commercial action updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new CommercialActionUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public CommercialAction complete(String publicId, User actor) {
        log.debug("Completing commercial action — publicId: {}, by: {}", publicId, actor.getUsername());

        CommercialAction action = getByPublicId(publicId);

        if (action.getStatus() != CommercialActionStatus.PENDING) {
            throw CommercialActionAlreadyTerminatedException.forAction(publicId, action.getStatus());
        }

        action.setStatus(CommercialActionStatus.DONE);
        action.setCompletedAt(Instant.now());

        CommercialAction saved = commercialActionRepository.save(action);
        log.info("Commercial action completed — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new CommercialActionCompletedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public CommercialAction cancel(String publicId, User actor) {
        log.debug("Cancelling commercial action — publicId: {}, by: {}", publicId, actor.getUsername());

        CommercialAction action = getByPublicId(publicId);

        if (action.getStatus() != CommercialActionStatus.PENDING) {
            throw CommercialActionAlreadyTerminatedException.forAction(publicId, action.getStatus());
        }

        action.setStatus(CommercialActionStatus.CANCELLED);

        CommercialAction saved = commercialActionRepository.save(action);
        log.info("Commercial action cancelled — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new CommercialActionCancelledEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public CommercialAction reassign(String publicId, String newAssigneePublicId, User actor) {
        log.debug("Reassigning commercial action {} to {}, by: {}",
                publicId, newAssigneePublicId, actor.getUsername());

        CommercialAction action = getByPublicId(publicId);
        User previousAssignee = action.getAssignedTo();

        User newAssignee = userRepository.findByPublicId(newAssigneePublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with publicId: " + newAssigneePublicId));

        action.setAssignedTo(newAssignee);
        CommercialAction saved = commercialActionRepository.save(action);

        log.info("Commercial action {} reassigned to {}, by: {}",
                publicId, newAssignee.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new CommercialActionReassignedEvent(
                saved, newAssignee, previousAssignee, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves a deal entity from its public UUID.
     * Returns {@code null} if {@code publicId} is null (action without deal).
     *
     * @param dealPublicId the public UUID of the deal, or {@code null}
     * @return the deal entity, or {@code null}
     * @throws IllegalArgumentException if the publicId is provided but yields no result
     */
    private Deal resolveDeal(String dealPublicId) {
        if (dealPublicId == null) return null;
        return dealRepository.findByPublicId(dealPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deal not found with publicId: " + dealPublicId));
    }

    /**
     * Resolves a contact entity from its public UUID.
     * Returns {@code null} if {@code publicId} is null (action without contact).
     *
     * @param contactPublicId the public UUID of the contact, or {@code null}
     * @return the contact entity, or {@code null}
     * @throws IllegalArgumentException if the publicId is provided but yields no result
     */
    private Contact resolveContact(String contactPublicId) {
        if (contactPublicId == null) return null;
        return contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));
    }
}
