package be.steby.CoreProject.bll.domains.organisation.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Organisation;

import java.time.Instant;

/**
 * Domain event published when an {@link Organisation} is updated.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the update in the activity log</li>
 *   <li>Propagate relevant field changes to linked contacts or deals</li>
 * </ul>
 *
 * @param organisation the organisation after the update
 * @param actor        the user who performed the action
 * @param actorDevice  the device from which the action was initiated
 * @param timestamp    when the event occurred
 */
public record OrganisationUpdatedEvent(
        Organisation organisation,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public OrganisationUpdatedEvent(Organisation organisation, User actor, Device actorDevice) {
        this(organisation, actor, actorDevice, Instant.now());
    }
}
