package be.steby.CoreProject.bll.domains.crm.call.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallSession;

import java.time.Instant;

/**
 * Domain event published when a {@link CallSession} is created and persisted.
 *
 * <p>Listeners may use this event to push a real-time notification to the
 * commercial's UI (e.g., to display the active call widget).</p>
 *
 * @param session   the newly created call session
 * @param actor     the commercial who initiated the call
 * @param timestamp when the event occurred
 */
public record CallInitiatedEvent(
        CallSession session,
        User actor,
        Instant timestamp
) {
    public CallInitiatedEvent(CallSession session, User actor) {
        this(session, actor, Instant.now());
    }
}
