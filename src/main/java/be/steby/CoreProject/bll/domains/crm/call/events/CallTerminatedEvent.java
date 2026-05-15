package be.steby.CoreProject.bll.domains.crm.call.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallSession;

import java.time.Instant;

/**
 * Domain event published when a {@link CallSession} reaches a terminal state
 * ({@code ENDED}, {@code MISSED}, or {@code FAILED}).
 *
 * <p>The {@code CallTerminatedInteractionListener} listens to this event and
 * creates the corresponding {@link be.steby.CoreProject.dl.entities.crm.Interaction}
 * and {@link be.steby.CoreProject.dl.entities.crm.CallLog} in the CRM timeline.</p>
 *
 * @param session   the terminated call session (already persisted with its final state)
 * @param actor     the commercial who terminated the call
 * @param timestamp when the event occurred
 */
public record CallTerminatedEvent(
        CallSession session,
        User actor,
        Instant timestamp
) {
    public CallTerminatedEvent(CallSession session, User actor) {
        this(session, actor, Instant.now());
    }
}
