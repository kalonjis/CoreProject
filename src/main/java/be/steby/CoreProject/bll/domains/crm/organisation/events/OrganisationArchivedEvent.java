package be.steby.CoreProject.bll.domains.crm.organisation.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Organisation;

import java.time.Instant;

/**
 * Domain event published when an {@link Organisation} is archived.
 *
 * <p>Archiving is a soft operation — the organisation is not deleted but
 * excluded from active CRM views. This event is typically triggered
 * as part of a merge operation, when the source organisation is archived
 * after all its contacts have been reassigned to the target.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the archival in the activity log</li>
 *   <li>Notify the commercial team</li>
 * </ul>
 *
 * @param organisation the organisation that was archived
 * @param actor        the user who performed the action
 * @param actorDevice  the device from which the action was initiated
 * @param timestamp    when the event occurred
 */
public record OrganisationArchivedEvent(
        Organisation organisation,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public OrganisationArchivedEvent(Organisation organisation, User actor, Device actorDevice) {
        this(organisation, actor, actorDevice, Instant.now());
    }
}
