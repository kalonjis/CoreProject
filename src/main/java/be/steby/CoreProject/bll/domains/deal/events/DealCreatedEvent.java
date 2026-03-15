package be.steby.CoreProject.bll.domains.deal.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;

import java.time.Instant;

/**
 * Domain event published when a {@link Deal} is manually created by a commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the creation in the activity log</li>
 *   <li>Send an internal notification to the assigned commercial</li>
 * </ul>
 *
 * @param deal        the newly created deal
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record DealCreatedEvent(
        Deal deal,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public DealCreatedEvent(Deal deal, User actor, Device actorDevice) {
        this(deal, actor, actorDevice, Instant.now());
    }
}
