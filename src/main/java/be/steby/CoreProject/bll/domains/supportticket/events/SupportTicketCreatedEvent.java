package be.steby.CoreProject.bll.domains.supportticket.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;

import java.time.Instant;

/**
 * Domain event published when a {@link SupportTicket} is created.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Send a confirmation notification to the contact</li>
 *   <li>Notify the support team of a new ticket</li>
 * </ul>
 *
 * @param ticket      the newly created ticket
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record SupportTicketCreatedEvent(
        SupportTicket ticket,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public SupportTicketCreatedEvent(SupportTicket ticket, User actor, Device actorDevice) {
        this(ticket, actor, actorDevice, Instant.now());
    }
}
