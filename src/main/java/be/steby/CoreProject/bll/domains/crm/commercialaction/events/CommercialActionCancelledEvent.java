package be.steby.CoreProject.bll.domains.crm.commercialaction.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;

import java.time.Instant;

/**
 * Domain event published when a {@link CommercialAction} transitions to status {@code CANCELLED}.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the cancellation in the activity log</li>
 *   <li>Notify the assigned commercial that the task has been cancelled</li>
 * </ul>
 *
 * @param action      the cancelled commercial action
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record CommercialActionCancelledEvent(
        CommercialAction action,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public CommercialActionCancelledEvent(CommercialAction action, User actor, Device actorDevice) {
        this(action, actor, actorDevice, Instant.now());
    }
}
