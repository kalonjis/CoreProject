package be.steby.CoreProject.bll.domains.crm.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} is archived.
 *
 * <p>Archiving is a soft operation — the contact is not deleted but
 * marked as {@link be.steby.CoreProject.dl.enums.crm.ContactStatus#INACTIVE}
 * and excluded from active CRM views.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the archival in the activity log</li>
 *   <li>Cancel any open tasks or reminders linked to the contact</li>
 *   <li>Notify the assigned commercial</li>
 * </ul>
 *
 * @param contact     the contact that was archived
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record ContactArchivedEvent(
        Contact contact,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactArchivedEvent(Contact contact, User actor, Device actorDevice) {
        this(contact, actor, actorDevice, Instant.now());
    }
}