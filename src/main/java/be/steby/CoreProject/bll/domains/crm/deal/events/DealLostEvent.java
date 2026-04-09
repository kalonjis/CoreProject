package be.steby.CoreProject.bll.domains.crm.deal.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;

import java.time.Instant;

/**
 * Domain event published when a {@link Deal} enters a terminal Lost stage,
 * setting its status to {@code LOST} and recording {@code closedAt}.
 *
 * <p>This event is published in addition to {@link DealStageChangedEvent}
 * when the new stage has {@code isLost = true}.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the loss in the activity log and reporting dashboard</li>
 *   <li>Trigger a post-mortem or loss reason collection workflow</li>
 *   <li>Notify the sales manager for review</li>
 * </ul>
 *
 * @param deal        the deal that was lost (status already set to LOST)
 * @param actor       the user who performed the stage move
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record DealLostEvent(
        Deal deal,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public DealLostEvent(Deal deal, User actor, Device actorDevice) {
        this(deal, actor, actorDevice, Instant.now());
    }
}
