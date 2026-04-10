package be.steby.CoreProject.bll.domains.crm.supportticket.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;

import java.time.Instant;

/**
 * Domain event published when a {@link SupportTicket} is closed.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Send a closure notification to the contact</li>
 *   <li>Record the resolution in the activity log</li>
 * </ul>
 *
 * @param ticket      the closed ticket
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record SupportTicketClosedEvent(
        SupportTicket ticket,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public SupportTicketClosedEvent(SupportTicket ticket, User actor, Device actorDevice) {
        this(ticket, actor, actorDevice, Instant.now());
    }
}
