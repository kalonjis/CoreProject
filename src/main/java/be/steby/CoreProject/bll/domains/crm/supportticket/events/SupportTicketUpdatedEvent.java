package be.steby.CoreProject.bll.domains.crm.supportticket.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;

import java.time.Instant;

/**
 * Domain event published when a {@link SupportTicket}'s content fields are updated.
 *
 * @param ticket      the updated ticket
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record SupportTicketUpdatedEvent(
        SupportTicket ticket,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public SupportTicketUpdatedEvent(SupportTicket ticket, User actor, Device actorDevice) {
        this(ticket, actor, actorDevice, Instant.now());
    }
}
