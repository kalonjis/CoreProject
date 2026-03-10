package be.steby.CoreProject.bll.domains.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} is updated.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the update in the activity log</li>
 *   <li>Propagate relevant field changes to linked entities (e.g., email change)</li>
 * </ul>
 *
 * @param contact     the contact after the update
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record ContactUpdatedEvent(
        Contact contact,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactUpdatedEvent(Contact contact, User actor, Device actorDevice) {
        this(contact, actor, actorDevice, Instant.now());
    }
}