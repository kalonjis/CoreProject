package be.steby.CoreProject.bll.domains.crm.commercialaction.events;

import be.steby.CoreProject.bll.domains.crm.commercialaction.models.CommercialActionCompleteRequest;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;

import java.time.Instant;

/**
 * Domain event published when a {@link CommercialAction} transitions to status {@code DONE}.
 *
 * <p>Listeners may use this event to:</p>
 * <ul>
 *   <li>Log the completion in the activity log</li>
 *   <li>Trigger follow-up workflows (e.g. create a new action or update the deal stage)</li>
 *   <li>Notify the commercial's manager</li>
 * </ul>
 *
 * @param action            the completed commercial action
 * @param actor             the user who performed the action
 * @param actorDevice       the device from which the action was initiated
 * @param completionDetails optional structured details (call or email) supplied at completion time
 * @param timestamp         when the event occurred
 */
public record CommercialActionCompletedEvent(
        CommercialAction action,
        User actor,
        Device actorDevice,
        CommercialActionCompleteRequest completionDetails,
        Instant timestamp
) {
    public CommercialActionCompletedEvent(CommercialAction action, User actor, Device actorDevice,
                                          CommercialActionCompleteRequest completionDetails) {
        this(action, actor, actorDevice, completionDetails, Instant.now());
    }
}
