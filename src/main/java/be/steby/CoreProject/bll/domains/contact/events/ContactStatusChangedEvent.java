package be.steby.CoreProject.bll.domains.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} transitions to a new CRM status.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the transition in the activity log</li>
 *   <li>Trigger notifications to the assigned commercial</li>
 *   <li>Drive automated pipeline actions (e.g., create a deal on QUALIFIED)</li>
 * </ul>
 *
 * @param contact         the contact after the status change
 * @param previousStatus  the status before the transition
 * @param newStatus       the status after the transition
 * @param actor           the user who performed the transition
 * @param actorDevice     the device from which the action was initiated
 * @param timestamp       when the event occurred
 */
public record ContactStatusChangedEvent(
        Contact contact,
        ContactStatus previousStatus,
        ContactStatus newStatus,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactStatusChangedEvent(
            Contact contact,
            ContactStatus previousStatus,
            ContactStatus newStatus,
            User actor,
            Device actorDevice
    ) {
        this(contact, previousStatus, newStatus, actor, actorDevice, Instant.now());
    }
}