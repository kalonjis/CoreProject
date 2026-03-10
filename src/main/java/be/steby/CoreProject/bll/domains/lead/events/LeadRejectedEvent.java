package be.steby.CoreProject.bll.domains.lead.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * Event published when a lead is rejected by a commercial.
 *
 * <p>This is the terminal failure event of the lead lifecycle.
 * A rejected lead cannot be reopened — a new lead must be submitted instead.</p>
 *
 * <p>Consumed by:</p>
 * <ul>
 *   <li>Notification listener — notifies the rejecting commercial for confirmation</li>
 * </ul>
 *
 * @param lead            the lead that was rejected
 * @param rejectedBy      the commercial who performed the rejection
 * @param rejectionReason the reason provided for the rejection
 * @param actorDevice     the device from which the rejection was performed
 */
public record LeadRejectedEvent(
        Lead lead,
        User rejectedBy,
        String rejectionReason,
        Device actorDevice
) {}