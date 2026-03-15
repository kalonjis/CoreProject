package be.steby.CoreProject.bll.domains.commercialaction.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;

import java.time.Instant;

/**
 * Domain event published when a {@link CommercialAction} is created by a commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the creation in the activity log</li>
 *   <li>Send an internal notification to the assigned commercial</li>
 * </ul>
 *
 * @param action      the newly created commercial action
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record CommercialActionCreatedEvent(
        CommercialAction action,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public CommercialActionCreatedEvent(CommercialAction action, User actor, Device actorDevice) {
        this(action, actor, actorDevice, Instant.now());
    }
}
