package be.steby.CoreProject.bll.domains.crm.call.services;

import be.steby.CoreProject.bll.domains.crm.call.events.CallInitiatedEvent;
import be.steby.CoreProject.bll.domains.crm.call.events.CallTerminatedEvent;
import be.steby.CoreProject.bll.domains.crm.call.exceptions.CallAlreadyActiveException;
import be.steby.CoreProject.bll.domains.crm.call.exceptions.CallSessionNotFoundException;
import be.steby.CoreProject.bll.domains.crm.call.exceptions.CallValidationException;
import be.steby.CoreProject.bll.domains.crm.call.models.InitiateCallRequest;
import be.steby.CoreProject.bll.domains.crm.call.models.TerminateCallRequest;
import be.steby.CoreProject.bll.domains.crm.contact.services.ContactService;
import be.steby.CoreProject.bll.domains.crm.lead.services.LeadService;
import be.steby.CoreProject.dal.repositories.crm.CallSessionRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import be.steby.CoreProject.il.telephony.TelephonyAdapterResolver;
import be.steby.CoreProject.il.telephony.TelephonyPort;
import be.steby.CoreProject.il.telephony.model.InitiateCallCommand;
import be.steby.CoreProject.il.telephony.model.TerminateCallCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Implementation of {@link CallService}.
 *
 * <h3>Initiation flow</h3>
 * <ol>
 *   <li>Validate request (contact or lead present, no active session for actor)</li>
 *   <li>Resolve adapter via {@link TelephonyAdapterResolver}</li>
 *   <li>Delegate to {@link TelephonyPort#initiate} → unpersisted session</li>
 *   <li>Persist and publish {@code CallInitiatedEvent}</li>
 * </ol>
 *
 * <h3>Termination flow</h3>
 * <ol>
 *   <li>Load session, validate it is not already terminal</li>
 *   <li>Delegate to {@link TelephonyPort#terminate} → session updated in-memory</li>
 *   <li>Persist and publish {@code CallTerminatedEvent}</li>
 *   <li>{@code CallTerminatedInteractionListener} creates the {@code Interaction} + {@code CallLog}</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class    CallServiceImpl implements CallService {

    private static final Set<CallSessionStatus> ACTIVE_STATUSES = Set.of(
            CallSessionStatus.INITIATED,
            CallSessionStatus.RINGING,
            CallSessionStatus.ACTIVE
    );

    private static final Set<CallSessionStatus> TERMINAL_STATUSES = Set.of(
            CallSessionStatus.ENDED,
            CallSessionStatus.MISSED,
            CallSessionStatus.FAILED
    );

    private final CallSessionRepository      callSessionRepository;
    private final ContactService             contactService;
    private final LeadService                leadService;
    private final TelephonyAdapterResolver   adapterResolver;
    private final ApplicationEventPublisher  eventPublisher;

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public CallSession getByPublicId(String publicId) {
        return callSessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> CallSessionNotFoundException.byPublicId(publicId));
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    @Transactional
    public CallSession initiate(InitiateCallRequest request, User actor) {
        validate(request);
        guardNoActiveSession(actor);

        Contact contact = resolveContact(request.contactPublicId());
        Lead    lead    = resolveLead(request.leadPublicId());

        TelephonyPort adapter = adapterResolver.resolve(actor);

        CallSession session = adapter.initiate(new InitiateCallCommand(
                request.phoneNumber(), contact, lead, actor));

        callSessionRepository.save(session);
        eventPublisher.publishEvent(new CallInitiatedEvent(session, actor));

        log.info("Call initiated — session: {}, provider: {}, number: {}, actor: {}",
                session.getPublicId(), session.getProvider(), request.phoneNumber(), actor.getId());

        return session;
    }

    @Override
    @Transactional
    public CallSession answer(String publicId, User actor) {
        CallSession session = getByPublicId(publicId);

        if (TERMINAL_STATUSES.contains(session.getStatus())) {
            throw new CallValidationException(
                    "CallSession '" + publicId + "' is already terminated — cannot mark as answered");
        }
        if (session.getStatus() == CallSessionStatus.ACTIVE) {
            throw new CallValidationException(
                    "CallSession '" + publicId + "' is already active");
        }

        session.setAnsweredAt(Instant.now());
        session.setStatus(CallSessionStatus.ACTIVE);
        callSessionRepository.save(session);

        log.info("Call answered — session: {}, actor: {}", session.getPublicId(), actor.getId());
        return session;
    }

    @Override
    @Transactional
    public void terminate(String publicId, TerminateCallRequest request, User actor) {
        CallSession session = getByPublicId(publicId);

        if (TERMINAL_STATUSES.contains(session.getStatus())) {
            throw new CallValidationException(
                    "CallSession '" + publicId + "' is already terminated with status: " + session.getStatus());
        }
        if (!TERMINAL_STATUSES.contains(request.status())) {
            throw new CallValidationException(
                    "Status '" + request.status() + "' is not a terminal status. Expected one of: " + TERMINAL_STATUSES);
        }

        TelephonyPort adapter = adapterResolver.resolveByProvider(session.getProvider());
        adapter.terminate(session, new TerminateCallCommand(request.status(), request.durationSeconds()));

        callSessionRepository.save(session);
        eventPublisher.publishEvent(new CallTerminatedEvent(session, actor));

        log.info("Call terminated — session: {}, status: {}, duration: {}s, actor: {}",
                session.getPublicId(), session.getStatus(), session.getDurationSeconds(), actor.getId());
    }

    @Override
    @Transactional
    public CallSession ring(String publicId, User actor) {
        CallSession session = getByPublicId(publicId);

        // Idempotent — skip if already past INITIATED
        if (session.getStatus() != CallSessionStatus.INITIATED) {
            return session;
        }

        session.setStatus(CallSessionStatus.RINGING);
        callSessionRepository.save(session);

        log.debug("Call ringing — session: {}, actor: {}", session.getPublicId(), actor.getId());
        return session;
    }

    @Override
    @Transactional
    public void registerExternalCallId(String publicId, String externalCallId) {
        CallSession session = getByPublicId(publicId);
        session.setExternalCallId(externalCallId);
        callSessionRepository.save(session);
        log.debug("ExternalCallId registered — session: {}, callSid: {}", publicId, externalCallId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void validate(InitiateCallRequest request) {
        if (request.contactPublicId() == null && request.leadPublicId() == null) {
            throw new CallValidationException("At least one of contactPublicId or leadPublicId must be provided");
        }
    }

    private void guardNoActiveSession(User actor) {
        if (callSessionRepository.existsByPerformedByIdAndStatusIn(actor.getId(), ACTIVE_STATUSES)) {
            throw CallAlreadyActiveException.forCommercial(actor.getId().toString());
        }
    }

    private Contact resolveContact(String publicId) {
        if (publicId == null) return null;
        return contactService.getByPublicId(publicId);
    }

    private Lead resolveLead(String publicId) {
        if (publicId == null) return null;
        return leadService.getByPublicId(publicId);
    }
}
