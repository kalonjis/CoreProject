package be.steby.CoreProject.bll.domains.crm.supportticket.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;

import java.time.Instant;

/**
 * Domain event published when a {@link SupportTicket} is permanently deleted.
 *
 * @param ticket      snapshot of the ticket before deletion (already removed from DB)
 * @param actor       the user who performed the deletion
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record SupportTicketDeletedEvent(
        SupportTicket ticket,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public SupportTicketDeletedEvent(SupportTicket ticket, User actor, Device actorDevice) {
        this(ticket, actor, actorDevice, Instant.now());
    }
}
