package be.steby.CoreProject.bll.domains.crm.deal.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.PipelineStep;

import java.time.Instant;

/**
 * Domain event published when a {@link Deal} is moved to a different
 * {@link PipelineStep} within its pipeline.
 *
 * <p>This event is always published on a stage change, regardless of whether
 * the new stage is terminal. Terminal stage changes additionally publish
 * {@link DealWonEvent} or {@link DealLostEvent}.</p>
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the stage transition in the activity log</li>
 *   <li>Trigger automated actions tied to specific stages (e.g., send quote)</li>
 *   <li>Update Kanban board state in real-time via SSE</li>
 * </ul>
 *
 * @param deal          the deal whose stage changed
 * @param previousStep  the pipeline step the deal was on before the move
 * @param newStep       the pipeline step the deal has moved to
 * @param actor         the user who performed the action
 * @param actorDevice   the device from which the action was initiated
 * @param timestamp     when the event occurred
 */
public record DealStageChangedEvent(
        Deal deal,
        PipelineStep previousStep,
        PipelineStep newStep,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public DealStageChangedEvent(Deal deal, PipelineStep previousStep, PipelineStep newStep,
                                  User actor, Device actorDevice) {
        this(deal, previousStep, newStep, actor, actorDevice, Instant.now());
    }
}
