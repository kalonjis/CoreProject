package be.steby.CoreProject.bll.domains.crm.interaction.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Interaction;

import java.time.Instant;

/**
 * Domain event published when an existing {@link Interaction} is partially updated by a commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the update in the activity log</li>
 *   <li>Notify relevant stakeholders of significant field changes</li>
 * </ul>
 *
 * @param interaction  the updated interaction (after changes)
 * @param actor        the user who performed the action
 * @param actorDevice  the device from which the action was initiated
 * @param timestamp    when the event occurred
 */
public record InteractionUpdatedEvent(
        Interaction interaction,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public InteractionUpdatedEvent(Interaction interaction, User actor, Device actorDevice) {
        this(interaction, actor, actorDevice, Instant.now());
    }
}
