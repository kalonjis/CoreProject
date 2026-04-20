package be.steby.CoreProject.bll.domains.crm.organisation.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Organisation;

import java.time.Instant;

/**
 * Domain event published when a new {@link Organisation} is created.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the creation in the activity log</li>
 *   <li>Notify the commercial team of the new organisation</li>
 * </ul>
 *
 * @param organisation the newly created organisation
 * @param actor        the user who performed the action
 * @param actorDevice  the device from which the action was initiated
 * @param timestamp    when the event occurred
 */
public record OrganisationCreatedEvent(
        Organisation organisation,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public OrganisationCreatedEvent(Organisation organisation, User actor, Device actorDevice) {
        this(organisation, actor, actorDevice, Instant.now());
    }
}
