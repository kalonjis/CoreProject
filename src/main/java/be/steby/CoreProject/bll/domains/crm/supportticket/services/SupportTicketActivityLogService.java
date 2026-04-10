package be.steby.CoreProject.bll.domains.crm.supportticket.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.crm.supportticket.events.*;
import be.steby.CoreProject.bll.domains.crm.supportticket.listeners.SupportTicketActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.SupportTicketAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CRM support ticket domain activity log service.
 *
 * <p>Translates support ticket domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to a {@link SupportTicketAction} constant.</p>
 *
 * <p>Called exclusively from {@link SupportTicketActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 *
 * <p>Note: {@link SupportTicketClosedEvent} is published alongside
 * {@link SupportTicketStatusChangedEvent} when the status becomes CLOSED.
 * Both are logged intentionally — the status change entry captures the transition,
 * while the CLOSED entry captures the terminal business outcome.</p>
 *
 * <p>{@code actionDetails} always begins with {@code "publicId:<value>"} so that
 * per-ticket queries can be performed via a LIKE filter on the existing
 * {@link be.steby.CoreProject.dl.entities.ActivityLog#getActionDetails()} column.</p>
 */
@Service
@Slf4j
public class SupportTicketActivityLogService extends ActivityLogService {

    public SupportTicketActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "CRM_SUPPORT";
    }

    // =========================================================================
    // Creation
    // =========================================================================

    /**
     * Persists a {@link SupportTicketAction#TICKET_CREATED} entry when a ticket is created.
     *
     * @param event contains the ticket, actor and device
     */
    public void logCreated(SupportTicketCreatedEvent event) {
        String details = "publicId:" + event.ticket().getPublicId()
                + " | subject: " + event.ticket().getSubject();
        logUserActivity(event.actor(), event.actorDevice(), SupportTicketAction.TICKET_CREATED, true, details);
        log.debug("TICKET_CREATED logged — ticket: {}", event.ticket().getPublicId());
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Persists a {@link SupportTicketAction#TICKET_STATUS_CHANGED} entry when
     * a ticket transitions to a new status. Also fires for CLOSED transitions.
     *
     * @param event contains the ticket, previous status, new status, actor and device
     */
    public void logStatusChanged(SupportTicketStatusChangedEvent event) {
        String details = "publicId:" + event.ticket().getPublicId()
                + " | status: " + event.previousStatus().name() + " → " + event.newStatus().name();
        logUserActivity(event.actor(), event.actorDevice(), SupportTicketAction.TICKET_STATUS_CHANGED, true, details);
        log.debug("TICKET_STATUS_CHANGED logged — ticket: {}, {} → {}",
                event.ticket().getPublicId(), event.previousStatus(), event.newStatus());
    }

    /**
     * Persists a {@link SupportTicketAction#TICKET_CLOSED} entry when a ticket
     * reaches its terminal CLOSED state.
     *
     * @param event contains the ticket, actor and device
     */
    public void logClosed(SupportTicketClosedEvent event) {
        String details = "publicId:" + event.ticket().getPublicId()
                + " | subject: " + event.ticket().getSubject();
        logUserActivity(event.actor(), event.actorDevice(), SupportTicketAction.TICKET_CLOSED, true, details);
        log.debug("TICKET_CLOSED logged — ticket: {}", event.ticket().getPublicId());
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Persists a {@link SupportTicketAction#TICKET_ASSIGNED} entry when a ticket
     * is assigned or reassigned to a team member.
     *
     * @param event contains the ticket, new/previous assignee, actor and device
     */
    public void logAssigned(SupportTicketAssignedEvent event) {
        String assigneeName = event.newAssignee() != null ? event.newAssignee().getUsername() : "none";
        String details = "publicId:" + event.ticket().getPublicId()
                + " | assignee: " + assigneeName;
        logUserActivity(event.actor(), event.actorDevice(), SupportTicketAction.TICKET_ASSIGNED, true, details);
        log.debug("TICKET_ASSIGNED logged — ticket: {}, assignee: {}",
                event.ticket().getPublicId(), assigneeName);
    }

    // =========================================================================
    // Deletion
    // =========================================================================

    /**
     * Persists a {@link SupportTicketAction#TICKET_DELETED} entry when a ticket
     * is permanently deleted. The ticket snapshot is used since it no longer exists in DB.
     *
     * @param event contains the ticket snapshot, actor and device
     */
    public void logDeleted(SupportTicketDeletedEvent event) {
        String details = "publicId:" + event.ticket().getPublicId()
                + " | subject: " + event.ticket().getSubject();
        logUserActivity(event.actor(), event.actorDevice(), SupportTicketAction.TICKET_DELETED, true, details);
        log.debug("TICKET_DELETED logged — ticket: {}", event.ticket().getPublicId());
    }
}
