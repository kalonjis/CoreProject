package be.steby.CoreProject.bll.domains.commercialaction.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;

import java.time.Instant;

/**
 * Domain event published when a {@link CommercialAction} is reassigned to a different commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the reassignment in the activity log</li>
 *   <li>Notify the newly assigned commercial</li>
 *   <li>Notify the previous assignee if relevant</li>
 * </ul>
 *
 * @param action           the commercial action being reassigned
 * @param newAssignee      the commercial now responsible for the action
 * @param previousAssignee the commercial previously responsible, or {@code null} if none
 * @param actor            the user who performed the action
 * @param actorDevice      the device from which the action was initiated
 * @param timestamp        when the event occurred
 */
public record CommercialActionReassignedEvent(
        CommercialAction action,
        User newAssignee,
        User previousAssignee,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public CommercialActionReassignedEvent(CommercialAction action, User newAssignee,
                                           User previousAssignee, User actor, Device actorDevice) {
        this(action, newAssignee, previousAssignee, actor, actorDevice, Instant.now());
    }
}
