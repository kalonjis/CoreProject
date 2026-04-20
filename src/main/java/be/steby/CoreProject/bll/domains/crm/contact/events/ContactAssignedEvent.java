package be.steby.CoreProject.bll.domains.crm.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} is assigned or reassigned
 * to a commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the assignment in the activity log</li>
 *   <li>Notify the newly assigned commercial</li>
 *   <li>Notify the previous assignee if relevant</li>
 * </ul>
 *
 * <h3>Unassignment</h3>
 * <p>When {@code newAssignee} is {@code null}, the contact has been unassigned.
 * {@code previousAssignee} carries who was removed.</p>
 *
 * @param contact          the contact being assigned
 * @param newAssignee      the commercial now responsible, or {@code null} if unassigned
 * @param previousAssignee the commercial previously responsible, or {@code null} if none
 * @param actor            the user who performed the action
 * @param actorDevice      the device from which the action was initiated
 * @param timestamp        when the event occurred
 */
public record ContactAssignedEvent(
        Contact contact,
        User newAssignee,
        User previousAssignee,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactAssignedEvent(Contact contact, User newAssignee, User previousAssignee, User actor, Device actorDevice) {
        this(contact, newAssignee, previousAssignee, actor, actorDevice, Instant.now());
    }
}