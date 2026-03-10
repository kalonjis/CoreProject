package be.steby.CoreProject.bll.domains.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Organisation;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} is linked to an {@link Organisation}.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the link in the activity log</li>
 *   <li>Notify the commercial assigned to the organisation</li>
 * </ul>
 *
 * @param contact          the contact that was linked
 * @param organisation     the organisation the contact was linked to
 * @param previousOrg      the organisation previously linked, or {@code null} if none
 * @param actor            the user who performed the action
 * @param actorDevice      the device from which the action was initiated
 * @param timestamp        when the event occurred
 */
public record ContactLinkedToOrganisationEvent(
        Contact contact,
        Organisation organisation,
        Organisation previousOrg,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactLinkedToOrganisationEvent(Contact contact, Organisation organisation, Organisation previousOrg, User actor, Device actorDevice) {
        this(contact, organisation, previousOrg, actor, actorDevice, Instant.now());
    }
}