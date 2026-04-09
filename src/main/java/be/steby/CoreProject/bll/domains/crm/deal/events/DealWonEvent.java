package be.steby.CoreProject.bll.domains.crm.deal.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;

import java.time.Instant;

/**
 * Domain event published when a {@link Deal} enters a terminal Won stage,
 * setting its status to {@code WON} and recording {@code closedAt}.
 *
 * <p>This event is published in addition to {@link DealStageChangedEvent}
 * when the new stage has {@code isWon = true}.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the win in the activity log and reporting dashboard</li>
 *   <li>Congratulate the assigned commercial</li>
 *   <li>Trigger post-sale onboarding workflows</li>
 * </ul>
 *
 * @param deal        the deal that was won (status already set to WON)
 * @param actor       the user who performed the stage move
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record DealWonEvent(
        Deal deal,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public DealWonEvent(Deal deal, User actor, Device actorDevice) {
        this(deal, actor, actorDevice, Instant.now());
    }
}
