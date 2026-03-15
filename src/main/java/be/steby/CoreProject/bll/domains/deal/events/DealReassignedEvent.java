package be.steby.CoreProject.bll.domains.deal.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;

import java.time.Instant;

/**
 * Domain event published when a {@link Deal} is reassigned to a different commercial.
 *
 * <h3>Unassignment</h3>
 * <p>When {@code newAssignee} is {@code null}, the deal has been unassigned.
 * {@code previousAssignee} carries who was removed.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the reassignment in the activity log</li>
 *   <li>Notify the newly assigned commercial</li>
 *   <li>Notify the previous assignee if relevant</li>
 * </ul>
 *
 * @param deal             the deal being reassigned
 * @param newAssignee      the commercial now responsible, or {@code null} if unassigned
 * @param previousAssignee the commercial previously responsible, or {@code null} if none
 * @param actor            the user who performed the action
 * @param actorDevice      the device from which the action was initiated
 * @param timestamp        when the event occurred
 */
public record DealReassignedEvent(
        Deal deal,
        User newAssignee,
        User previousAssignee,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public DealReassignedEvent(Deal deal, User newAssignee, User previousAssignee,
                                User actor, Device actorDevice) {
        this(deal, newAssignee, previousAssignee, actor, actorDevice, Instant.now());
    }
}
