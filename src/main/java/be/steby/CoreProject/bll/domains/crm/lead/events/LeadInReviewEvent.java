package be.steby.CoreProject.bll.domains.crm.lead.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * Event published when a lead transitions to {@code IN_REVIEW} status.
 *
 * <p>Fired when a commercial opens a lead and starts evaluating it.
 * This is the first manual action in the CRM qualification flow.</p>
 *
 * <p>Consumed by:</p>
 * <ul>
 *   <li>Notification listener — notifies admins or team leads if relevant</li>
 * </ul>
 *
 * @param lead        the lead now under review
 * @param reviewedBy  the commercial who opened the lead
 * @param actorDevice the device from which the action was performed
 */
public record LeadInReviewEvent(
        Lead lead,
        User reviewedBy,
        Device actorDevice
) {}