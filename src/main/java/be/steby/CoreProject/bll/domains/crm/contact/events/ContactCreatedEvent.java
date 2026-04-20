package be.steby.CoreProject.bll.domains.crm.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} is manually created
 * by a commercial — not via lead conversion.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the creation in the activity log</li>
 *   <li>Send an internal notification to the assigned commercial</li>
 * </ul>
 *
 * @param contact     the newly created contact
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record ContactCreatedEvent(
        Contact contact,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactCreatedEvent(Contact contact, User actor, Device actorDevice) {
        this(contact, actor, actorDevice, Instant.now());
    }
}