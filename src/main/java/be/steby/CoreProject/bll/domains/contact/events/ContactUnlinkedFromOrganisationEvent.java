package be.steby.CoreProject.bll.domains.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Organisation;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} is unlinked from its
 * {@link Organisation}, making it an independent contact.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the unlink in the activity log</li>
 *   <li>Notify the commercial assigned to the organisation</li>
 *   <li>Flag open deals linked to both this contact and the organisation for review</li>
 * </ul>
 *
 * @param contact         the contact that was unlinked
 * @param previousOrg     the organisation the contact was unlinked from
 * @param actor           the user who performed the action
 * @param actorDevice     the device from which the action was initiated
 * @param timestamp       when the event occurred
 */
public record ContactUnlinkedFromOrganisationEvent(
        Contact contact,
        Organisation previousOrg,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactUnlinkedFromOrganisationEvent(Contact contact, Organisation previousOrg, User actor, Device actorDevice) {
        this(contact, previousOrg, actor, actorDevice, Instant.now());
    }
}