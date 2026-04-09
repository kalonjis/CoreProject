package be.steby.CoreProject.bll.domains.crm.lead.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * Event published when a lead is assigned to a commercial.
 *
 * <p>Fired in two cases:</p>
 * <ul>
 *   <li>Initial assignment — {@code previousAssignee} is {@code null}</li>
 *   <li>Reassignment — {@code previousAssignee} holds the former commercial</li>
 * </ul>
 *
 * <p>Consumed by:</p>
 * <ul>
 *   <li>Notification listener — notifies the newly assigned commercial</li>
 * </ul>
 *
 * @param lead             the lead that was assigned
 * @param assignedTo       the commercial now responsible for this lead
 * @param previousAssignee the commercial previously assigned, or {@code null} if first assignment
 * @param actorDevice      the device from which the assignment was performed
 */
public record LeadAssignedEvent(
        Lead lead,
        User assignedTo,
        User previousAssignee,
        Device actorDevice
) {

    /**
     * Returns {@code true} if this is a reassignment (lead had a previous assignee).
     *
     * @return true if the lead was already assigned to someone else
     */
    public boolean isReassignment() {
        return previousAssignee != null;
    }
}