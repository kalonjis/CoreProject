package be.steby.CoreProject.bll.domains.supportticket.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;

import java.time.Instant;

/**
 * Domain event published when a {@link SupportTicket}'s status changes.
 *
 * @param ticket         the updated ticket
 * @param previousStatus the status before the transition
 * @param newStatus      the status after the transition
 * @param actor          the user who performed the action
 * @param actorDevice    the device from which the action was initiated
 * @param timestamp      when the event occurred
 */
public record SupportTicketStatusChangedEvent(
        SupportTicket ticket,
        SupportTicketStatus previousStatus,
        SupportTicketStatus newStatus,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public SupportTicketStatusChangedEvent(
            SupportTicket ticket,
            SupportTicketStatus previousStatus,
            SupportTicketStatus newStatus,
            User actor,
            Device actorDevice) {
        this(ticket, previousStatus, newStatus, actor, actorDevice, Instant.now());
    }
}
