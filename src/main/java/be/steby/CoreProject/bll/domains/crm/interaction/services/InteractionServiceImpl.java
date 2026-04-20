package be.steby.CoreProject.bll.domains.crm.interaction.services;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.crm.interaction.events.InteractionCreatedEvent;
import be.steby.CoreProject.bll.domains.crm.interaction.events.InteractionDeletedEvent;
import be.steby.CoreProject.bll.domains.crm.interaction.events.InteractionUpdatedEvent;
import be.steby.CoreProject.bll.domains.crm.interaction.exceptions.InteractionNotFoundException;
import be.steby.CoreProject.bll.domains.crm.interaction.exceptions.InteractionValidationException;
import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionUpdateRequest;
import be.steby.CoreProject.dal.repositories.crm.CallLogRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.EmailLogRepository;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallLog;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.EmailLog;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link InteractionService}.
 *
 * <h3>Creation flow</h3>
 * <p>{@link #create(InteractionCreateRequest, User)} validates the request,
 * resolves deal/contact/lead references, persists the {@link Interaction}, then
 * conditionally creates a {@link CallLog} or {@link EmailLog} sub-entity based
 * on the interaction type before publishing {@code InteractionCreatedEvent}.</p>
 *
 * <h3>Type-specific sub-entity handling</h3>
 * <p>{@code CALL} and {@code EMAIL} types require their respective detail records.
 * The service validates presence of these details and rejects the request with
 * {@link InteractionValidationException} if the constraint is violated.</p>
 *
 * <h3>Cascade delete</h3>
 * <p>Deleting an interaction automatically removes the associated {@link CallLog}
 * or {@link EmailLog} via JPA cascade ({@code CascadeType.ALL} + {@code orphanRemoval}).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InteractionServiceImpl implements InteractionService {

    private final InteractionRepository    interactionRepository;
    private final CallLogRepository        callLogRepository;
    private final EmailLogRepository       emailLogRepository;
    private final DealRepository           dealRepository;
    private final ContactRepository        contactRepository;
    private final LeadRepository           leadRepository;
    private final DeviceService            deviceService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public Interaction getByPublicId(String publicId) {
        return interactionRepository.findByPublicId(publicId)
                .orElseThrow(() -> InteractionNotFoundException.byPublicId(publicId));
    }

    // =========================================================================
    // Write
    // =========================================================================

    @Override
    @Transactional
    public Interaction create(InteractionCreateRequest request, User actor) {
        log.debug("Creating interaction — type: {}, subject: '{}', by: {}",
                request.type(), request.subject(), actor.getUsername());

        // Guard: at least one of lead/deal/contact must be provided
        if (request.dealPublicId() == null && request.contactPublicId() == null && request.leadPublicId() == null) {
            throw InteractionValidationException.neitherDealNorContact();
        }

        // Guard: type-specific detail requirements
        if (request.type() == InteractionType.CALL && request.callLogDetails() == null) {
            throw InteractionValidationException.callLogRequiredForCallType();
        }
        if (request.type() == InteractionType.EMAIL && request.emailLogDetails() == null) {
            throw InteractionValidationException.emailLogRequiredForEmailType();
        }

        Deal    deal    = resolveDeal(request.dealPublicId());
        Contact contact = resolveContact(request.contactPublicId());
        Lead    lead    = resolveLead(request.leadPublicId());

        Interaction interaction = Interaction.builder()
                .type(request.type())
                .direction(request.direction())
                .subject(request.subject())
                .notes(request.notes())
                .outcome(request.outcome())
                .durationMinutes(request.durationMinutes())
                .occurredAt(request.occurredAt())
                .deal(deal)
                .contact(contact)
                .lead(lead)
                .performedBy(actor)
                .build();

        interaction.setPublicId(UUID.randomUUID().toString());

        Interaction saved = interactionRepository.save(interaction);
        log.info("Interaction created — publicId: {}, type: {}, by: {}",
                saved.getPublicId(), saved.getType(), actor.getUsername());

        if (request.type() == InteractionType.CALL) {
            InteractionCreateRequest.CallLogDetails details = request.callLogDetails();
            CallLog callLog = CallLog.builder()
                    .phoneNumber(details.phoneNumber())
                    .durationSeconds(details.durationSeconds())
                    .status(details.status())
                    .recordingUrl(details.recordingUrl())
                    .interaction(saved)
                    .build();
            callLog.setPublicId(UUID.randomUUID().toString());
            callLogRepository.save(callLog);
            log.debug("CallLog saved for interaction publicId: {}", saved.getPublicId());
        }

        if (request.type() == InteractionType.EMAIL) {
            InteractionCreateRequest.EmailLogDetails details = request.emailLogDetails();
            EmailLog emailLog = EmailLog.builder()
                    .subject(details.emailSubject())
                    .bodySnippet(details.bodySnippet())
                    .externalMessageId(details.externalMessageId())
                    .interaction(saved)
                    .build();
            emailLog.setPublicId(UUID.randomUUID().toString());
            emailLogRepository.save(emailLog);
            log.debug("EmailLog saved for interaction publicId: {}", saved.getPublicId());
        }

        eventPublisher.publishEvent(new InteractionCreatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public Interaction update(String publicId, InteractionUpdateRequest request, User actor) {
        log.debug("Updating interaction — publicId: {}, by: {}", publicId, actor.getUsername());

        Interaction interaction = getByPublicId(publicId);

        if (request.subject()         != null) interaction.setSubject(request.subject());
        if (request.notes()           != null) interaction.setNotes(request.notes());
        if (request.outcome()         != null) interaction.setOutcome(request.outcome());
        if (request.durationMinutes() != null) interaction.setDurationMinutes(request.durationMinutes());
        if (request.occurredAt()      != null) interaction.setOccurredAt(request.occurredAt());

        Interaction saved = interactionRepository.save(interaction);
        log.info("Interaction updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new InteractionUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public void delete(String publicId, User actor) {
        log.debug("Deleting interaction — publicId: {}, by: {}", publicId, actor.getUsername());

        Interaction interaction = getByPublicId(publicId);
        interactionRepository.delete(interaction);

        log.info("Interaction deleted — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new InteractionDeletedEvent(
                publicId, actor, deviceService.detectAndRegisterDevice(actor)));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Deal resolveDeal(String dealPublicId) {
        if (dealPublicId == null) return null;
        return dealRepository.findByPublicId(dealPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deal not found with publicId: " + dealPublicId));
    }

    private Contact resolveContact(String contactPublicId) {
        if (contactPublicId == null) return null;
        return contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));
    }

    private Lead resolveLead(String leadPublicId) {
        if (leadPublicId == null) return null;
        return leadRepository.findByPublicId(leadPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lead not found with publicId: " + leadPublicId));
    }
}
