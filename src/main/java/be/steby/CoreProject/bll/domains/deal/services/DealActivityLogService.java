package be.steby.CoreProject.bll.domains.deal.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.deal.events.*;
import be.steby.CoreProject.bll.domains.deal.listeners.DealActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.DealAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CRM deal domain activity log service.
 *
 * <p>Translates deal domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to a {@link DealAction} constant.</p>
 *
 * <p>Called exclusively from {@link DealActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 *
 * <p>Note: {@link DealWonEvent} and {@link DealLostEvent} are published alongside
 * {@link DealStageChangedEvent} for terminal stage moves. Both are logged intentionally —
 * the stage move entry captures the transition context, while the WON/LOST entry
 * captures the business outcome.</p>
 *
 * <p>{@code actionDetails} always begins with {@code "publicId:<value>"} so that
 * per-deal queries can be performed via a LIKE filter on the existing
 * {@link be.steby.CoreProject.dl.entities.ActivityLog#getActionDetails()} column.</p>
 */
@Service
@Slf4j
public class DealActivityLogService extends ActivityLogService {

    public DealActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "CRM_DEAL";
    }

    // =========================================================================
    // Creation
    // =========================================================================

    /**
     * Persists a {@link DealAction#DEAL_CREATED} entry when a deal is created.
     *
     * @param event contains the new deal, actor and device
     */
    public void logCreated(DealCreatedEvent event) {
        String details = "publicId:" + event.deal().getPublicId()
                + " | title: " + event.deal().getTitle();
        logUserActivity(event.actor(), event.actorDevice(), DealAction.DEAL_CREATED, true, details);
        log.debug("DEAL_CREATED logged — deal: {}", event.deal().getPublicId());
    }

    // =========================================================================
    // Stage
    // =========================================================================

    /**
     * Persists a {@link DealAction#DEAL_STAGE_MOVED} entry when a deal moves
     * to a new pipeline stage. Also fires for terminal WON/LOST moves.
     *
     * @param event contains the deal, previous step, new step, actor and device
     */
    public void logStageMoved(DealStageChangedEvent event) {
        String details = "publicId:" + event.deal().getPublicId()
                + " | stage: " + event.previousStep().getName() + " → " + event.newStep().getName();
        logUserActivity(event.actor(), event.actorDevice(), DealAction.DEAL_STAGE_MOVED, true, details);
        log.debug("DEAL_STAGE_MOVED logged — deal: {}, {} → {}",
                event.deal().getPublicId(), event.previousStep().getName(), event.newStep().getName());
    }

    /**
     * Persists a {@link DealAction#DEAL_WON} entry when a deal is marked as won.
     *
     * @param event contains the deal (status already WON), actor and device
     */
    public void logWon(DealWonEvent event) {
        String details = "publicId:" + event.deal().getPublicId()
                + " | title: " + event.deal().getTitle();
        logUserActivity(event.actor(), event.actorDevice(), DealAction.DEAL_WON, true, details);
        log.debug("DEAL_WON logged — deal: {}", event.deal().getPublicId());
    }

    /**
     * Persists a {@link DealAction#DEAL_LOST} entry when a deal is marked as lost.
     * Includes the loss reason in the details when available.
     *
     * @param event contains the deal (status already LOST), actor and device
     */
    public void logLost(DealLostEvent event) {
        String lostReason = event.deal().getLostReason() != null
                ? event.deal().getLostReason()
                : "no reason provided";
        String details = "publicId:" + event.deal().getPublicId()
                + " | reason: " + lostReason;
        logUserActivity(event.actor(), event.actorDevice(), DealAction.DEAL_LOST, true, details);
        log.debug("DEAL_LOST logged — deal: {}, reason: {}", event.deal().getPublicId(), lostReason);
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Persists a {@link DealAction#DEAL_REASSIGNED} entry when a deal is
     * reassigned to a different commercial.
     *
     * @param event contains the deal, new/previous assignee, actor and device
     */
    public void logReassigned(DealReassignedEvent event) {
        String assigneeName = event.newAssignee() != null ? event.newAssignee().getUsername() : "none";
        String details = "publicId:" + event.deal().getPublicId()
                + " | assignee: " + assigneeName;
        logUserActivity(event.actor(), event.actorDevice(), DealAction.DEAL_REASSIGNED, true, details);
        log.debug("DEAL_REASSIGNED logged — deal: {}, assignee: {}",
                event.deal().getPublicId(), assigneeName);
    }
}
