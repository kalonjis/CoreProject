package be.steby.CoreProject.bll.domains.crm.organisation.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Organisation;

import java.time.Instant;

/**
 * Domain event published when two duplicate {@link Organisation} entries are merged.
 *
 * <p>The {@code sourceOrganisation} has been archived after all its linked
 * contacts were reassigned to the {@code targetOrganisation}.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the merge in the activity log</li>
 *   <li>Notify the commercial team of the surviving organisation</li>
 *   <li>Trigger any downstream deduplication logic</li>
 * </ul>
 *
 * @param targetOrganisation the surviving organisation after the merge
 * @param sourceOrganisation the archived organisation that was merged into the target
 * @param actor              the user who performed the action
 * @param actorDevice        the device from which the action was initiated
 * @param timestamp          when the event occurred
 */
public record OrganisationMergedEvent(
        Organisation targetOrganisation,
        Organisation sourceOrganisation,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public OrganisationMergedEvent(Organisation targetOrganisation, Organisation sourceOrganisation, User actor, Device actorDevice) {
        this(targetOrganisation, sourceOrganisation, actor, actorDevice, Instant.now());
    }
}
