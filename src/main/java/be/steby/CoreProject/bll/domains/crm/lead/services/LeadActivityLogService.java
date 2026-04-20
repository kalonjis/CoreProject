package be.steby.CoreProject.bll.domains.crm.lead.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.crm.lead.events.*;
import be.steby.CoreProject.bll.domains.crm.lead.listeners.LeadActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.LeadAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CRM lead domain activity log service.
 *
 * <p>Translates lead domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to a {@link LeadAction} constant.</p>
 *
 * <p>Called exclusively from {@link LeadActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 *
 * <p>Note on {@link LeadSubmittedEvent}: this event originates from a public form
 * with no authenticated session. The log entry is persisted with {@code user = null}
 * and {@code device = null}, which is valid per {@link ActivityLogService} contract.</p>
 *
 * <p>Note on {@link LeadAssignedEvent}: the event does not carry an explicit actor field —
 * {@code assignedTo} is used as the user reference for the log entry.</p>
 *
 * <p>{@code actionDetails} always begins with {@code "publicId:<value>"} so that
 * per-lead queries can be performed via a LIKE filter on the existing
 * {@link be.steby.CoreProject.dl.entities.ActivityLog#getActionDetails()} column.</p>
 */
@Service
@Slf4j
public class LeadActivityLogService extends ActivityLogService {

    public LeadActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "CRM_LEAD";
    }

    // =========================================================================
    // Submission
    // =========================================================================

    /**
     * Persists a {@link LeadAction#LEAD_SUBMITTED} entry when a lead arrives
     * via public contact form or webhook. No authenticated user is involved.
     *
     * @param event contains the lead (no user or device — public flow)
     */
    public void logSubmitted(LeadSubmittedEvent event) {
        String source = event.lead().getLeadSource() != null
                ? event.lead().getLeadSource().name() : "UNKNOWN";
        String details = "publicId:" + event.lead().getPublicId()
                + " | source: " + source;
        logUserActivity(null, null, LeadAction.LEAD_SUBMITTED, true, details);
        log.debug("LEAD_SUBMITTED logged — lead: {}", event.lead().getPublicId());
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Persists a {@link LeadAction#LEAD_ASSIGNED} entry when a lead is assigned
     * or reassigned to a commercial.
     *
     * <p>The {@code assignedTo} user is used as the actor since {@link LeadAssignedEvent}
     * does not carry a separate actor field.</p>
     *
     * @param event contains the lead, assignee and device
     */
    public void logAssigned(LeadAssignedEvent event) {
        String details = "publicId:" + event.lead().getPublicId()
                + " | assignee: " + event.assignedTo().getUsername();
        logUserActivity(event.assignedTo(), event.actorDevice(), LeadAction.LEAD_ASSIGNED, true, details);
        log.debug("LEAD_ASSIGNED logged — lead: {}, assignee: {}",
                event.lead().getPublicId(), event.assignedTo().getUsername());
    }

    /**
     * Persists a {@link LeadAction#LEAD_IN_REVIEW} entry when a commercial
     * opens and starts evaluating a lead.
     *
     * @param event contains the lead, reviewer and device
     */
    public void logInReview(LeadInReviewEvent event) {
        String details = "publicId:" + event.lead().getPublicId();
        logUserActivity(event.reviewedBy(), event.actorDevice(), LeadAction.LEAD_IN_REVIEW, true, details);
        log.debug("LEAD_IN_REVIEW logged — lead: {}", event.lead().getPublicId());
    }

    /**
     * Persists a {@link LeadAction#LEAD_CONVERTED} entry when a lead is
     * successfully converted into a contact.
     *
     * @param event contains the lead, converter user, resulting contact publicId and device
     */
    public void logConverted(LeadConvertedEvent event) {
        String details = "publicId:" + event.lead().getPublicId()
                + " | contact: " + event.contactPublicId();
        logUserActivity(event.convertedBy(), event.actorDevice(), LeadAction.LEAD_CONVERTED, true, details);
        log.debug("LEAD_CONVERTED logged — lead: {}, contact: {}",
                event.lead().getPublicId(), event.contactPublicId());
    }

    /**
     * Persists a {@link LeadAction#LEAD_REJECTED} entry when a commercial
     * rejects a lead with a reason.
     *
     * @param event contains the lead, rejector user, rejection reason and device
     */
    public void logRejected(LeadRejectedEvent event) {
        String details = "publicId:" + event.lead().getPublicId()
                + " | reason: " + event.rejectionReason();
        logUserActivity(event.rejectedBy(), event.actorDevice(), LeadAction.LEAD_REJECTED, true, details);
        log.debug("LEAD_REJECTED logged — lead: {}, reason: {}",
                event.lead().getPublicId(), event.rejectionReason());
    }
}
