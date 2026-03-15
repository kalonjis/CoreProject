package be.steby.CoreProject.bll.domains.deal.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;

import java.time.Instant;

/**
 * Domain event published when a {@link Deal} is partially updated by a commercial.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the update in the activity log</li>
 *   <li>Notify relevant stakeholders of significant field changes</li>
 * </ul>
 *
 * @param deal        the updated deal (after changes)
 * @param actor       the user who performed the action
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record DealUpdatedEvent(
        Deal deal,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public DealUpdatedEvent(Deal deal, User actor, Device actorDevice) {
        this(deal, actor, actorDevice, Instant.now());
    }
}
