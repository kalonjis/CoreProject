package be.steby.CoreProject.bll.domains.interaction.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Domain event published when an {@link be.steby.CoreProject.dl.entities.crm.Interaction}
 * is permanently deleted by a commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the deletion in the activity audit trail</li>
 *   <li>Clean up any derived data or cached timeline entries</li>
 * </ul>
 *
 * <p>Note: the full entity is no longer available at publish time — only the
 * {@code publicId} is retained for audit purposes.</p>
 *
 * @param interactionPublicId  the public UUID of the deleted interaction
 * @param actor                the user who performed the deletion
 * @param actorDevice          the device from which the action was initiated
 * @param timestamp            when the event occurred
 */
public record InteractionDeletedEvent(
        String interactionPublicId,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public InteractionDeletedEvent(String interactionPublicId, User actor, Device actorDevice) {
        this(interactionPublicId, actor, actorDevice, Instant.now());
    }
}
