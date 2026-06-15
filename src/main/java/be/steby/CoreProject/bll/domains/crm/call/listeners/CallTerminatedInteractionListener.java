package be.steby.CoreProject.bll.domains.crm.call.listeners;

import be.steby.CoreProject.bll.domains.crm.call.events.CallTerminatedEvent;
import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.bll.domains.crm.interaction.services.InteractionService;
import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a {@link be.steby.CoreProject.dl.entities.crm.Interaction} and its
 * {@link be.steby.CoreProject.dl.entities.crm.CallLog} when a call session terminates.
 *
 * <h3>Domain ownership</h3>
 * <p>Persisting interactions belongs to the {@code interaction} domain.
 * The {@code call} domain only publishes {@link CallTerminatedEvent} — it has no
 * knowledge of interaction storage. This listener bridges the two domains.</p>
 *
 * <h3>Status mapping</h3>
 * <ul>
 *   <li>{@code ENDED}  → {@link CallStatus#ANSWERED},  {@link InteractionOutcome#NEUTRAL}</li>
 *   <li>{@code MISSED} → {@link CallStatus#NO_ANSWER},  {@link InteractionOutcome#NO_ANSWER}</li>
 *   <li>{@code FAILED} → {@link CallStatus#NO_ANSWER},  {@link InteractionOutcome#NO_ANSWER}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class    CallTerminatedInteractionListener {

    private final InteractionService interactionService;

    @EventListener
    @Transactional
    public void onCallTerminated(CallTerminatedEvent event) {
        CallSession session = event.session();

        log.debug("Call terminated event received — session: {}, status: {}",
                session.getPublicId(), session.getStatus());

        // Inbound call with no linked contact or lead — skip interaction (caller unknown)
        if (session.getContact() == null && session.getLead() == null) {
            log.info("Skipping interaction for call session {} — no contact or lead linked (inbound/unknown caller)",
                    session.getPublicId());
            return;
        }

        InteractionCreateRequest request = new InteractionCreateRequest(
                InteractionType.CALL,
                InteractionDirection.OUTBOUND,
                "Outbound call — " + session.getPhoneNumber(),
                null,
                resolveOutcome(session.getStatus()),
                computeDurationMinutes(session.getDurationSeconds()),
                session.getStartedAt(),
                null,
                session.getContact() != null ? session.getContact().getPublicId() : null,
                session.getLead()    != null ? session.getLead().getPublicId()    : null,
                new InteractionCreateRequest.CallLogDetails(
                        session.getPhoneNumber(),
                        session.getDurationSeconds(),
                        resolveCallStatus(session.getStatus()),
                        null
                ),
                null
        );

        interactionService.create(request, event.actor());

        log.info("Interaction logged from call session: {} — status: {}",
                session.getPublicId(), session.getStatus());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private CallStatus resolveCallStatus(CallSessionStatus status) {
        return status == CallSessionStatus.ENDED ? CallStatus.ANSWERED : CallStatus.NO_ANSWER;
    }

    private InteractionOutcome resolveOutcome(CallSessionStatus status) {
        return status == CallSessionStatus.ENDED ? InteractionOutcome.NEUTRAL : InteractionOutcome.NO_ANSWER;
    }

    private Integer computeDurationMinutes(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds == 0) return null;
        return Math.max(1, durationSeconds / 60);
    }
}
