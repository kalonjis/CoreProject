package be.steby.CoreProject.bll.domains.interaction.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Interaction;

import java.time.Instant;

/**
 * Domain event published when a new {@link Interaction} is logged by a commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the creation in the activity log</li>
 *   <li>Send an internal notification to relevant stakeholders</li>
 *   <li>Trigger follow-up task creation (e.g. on NO_ANSWER calls)</li>
 * </ul>
 *
 * @param interaction  the newly created interaction
 * @param actor        the user who performed the action
 * @param actorDevice  the device from which the action was initiated
 * @param timestamp    when the event occurred
 */
public record InteractionCreatedEvent(
        Interaction interaction,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public InteractionCreatedEvent(Interaction interaction, User actor, Device actorDevice) {
        this(interaction, actor, actorDevice, Instant.now());
    }
}
