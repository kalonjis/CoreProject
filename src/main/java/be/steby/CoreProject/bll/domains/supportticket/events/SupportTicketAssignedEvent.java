package be.steby.CoreProject.bll.domains.supportticket.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;

import java.time.Instant;

/**
 * Domain event published when a {@link SupportTicket} is assigned or reassigned.
 *
 * @param ticket           the updated ticket
 * @param newAssignee      the newly assigned user, or {@code null} if unassigned
 * @param previousAssignee the previously assigned user, or {@code null} if none
 * @param actor            the user who performed the action
 * @param actorDevice      the device from which the action was initiated
 * @param timestamp        when the event occurred
 */
public record SupportTicketAssignedEvent(
        SupportTicket ticket,
        User newAssignee,
        User previousAssignee,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public SupportTicketAssignedEvent(
            SupportTicket ticket,
            User newAssignee,
            User previousAssignee,
            User actor,
            Device actorDevice) {
        this(ticket, newAssignee, previousAssignee, actor, actorDevice, Instant.now());
    }
}
