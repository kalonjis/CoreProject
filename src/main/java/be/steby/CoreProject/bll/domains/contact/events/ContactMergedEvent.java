package be.steby.CoreProject.bll.domains.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;

import java.time.Instant;

/**
 * Domain event published when two duplicate {@link Contact} entries are merged.
 *
 * <p>The {@code sourceContact} has been archived after all its related records
 * (deals, interactions) were reassigned to the {@code targetContact}.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the merge in the activity log</li>
 *   <li>Notify the assigned commercial of the surviving contact</li>
 *   <li>Trigger any downstream deduplication logic</li>
 * </ul>
 *
 * @param targetContact the surviving contact after the merge
 * @param sourceContact the archived contact that was merged into the target
 * @param actor         the user who performed the action
 * @param actorDevice   the device from which the action was initiated
 * @param timestamp     when the event occurred
 */
public record ContactMergedEvent(
        Contact targetContact,
        Contact sourceContact,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactMergedEvent(Contact targetContact, Contact sourceContact, User actor, Device actorDevice) {
        this(targetContact, sourceContact, actor, actorDevice, Instant.now());
    }
}