package be.steby.CoreProject.bll.domains.crm.commercialaction.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;

import java.time.Instant;

/**
 * Domain event published when a {@link CommercialAction} is partially updated.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the update in the activity log</li>
 *   <li>Notify relevant stakeholders of the change</li>
 * </ul>
 *
 * @param action      the updated commercial action
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record CommercialActionUpdatedEvent(
        CommercialAction action,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public CommercialActionUpdatedEvent(CommercialAction action, User actor, Device actorDevice) {
        this(action, actor, actorDevice, Instant.now());
    }
}
