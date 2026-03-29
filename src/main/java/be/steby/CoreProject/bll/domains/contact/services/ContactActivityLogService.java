package be.steby.CoreProject.bll.domains.contact.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.contact.events.*;
import be.steby.CoreProject.bll.domains.contact.listeners.ContactActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.ContactAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CRM contact domain activity log service.
 *
 * <p>Translates contact domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to a {@link ContactAction} constant.</p>
 *
 * <p>Called exclusively from {@link ContactActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 *
 * <p>{@code actionDetails} always begins with {@code "publicId:<value>"} so that
 * per-contact queries can be performed via a LIKE filter on the existing
 * {@link be.steby.CoreProject.dl.entities.ActivityLog#getActionDetails()} column.</p>
 */
@Service
@Slf4j
public class ContactActivityLogService extends ActivityLogService {

    public ContactActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "CRM_CONTACT";
    }

    // =========================================================================
    // Creation
    // =========================================================================

    /**
     * Persists a {@link ContactAction#CONTACT_CREATED} entry when a contact
     * is manually created by a commercial.
     *
     * @param event contains the new contact, actor and device
     */
    public void logCreated(ContactCreatedEvent event) {
        String details = "publicId:" + event.contact().getPublicId();
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_CREATED, true, details);
        log.debug("CONTACT_CREATED logged — contact: {}", event.contact().getPublicId());
    }

    /**
     * Persists a {@link ContactAction#CONTACT_CREATED_FROM_LEAD} entry when a
     * contact is created as a result of a lead conversion.
     *
     * @param event contains the new contact, originating lead, actor and device
     */
    public void logCreatedFromLead(ContactCreatedFromLeadEvent event) {
        String details = "publicId:" + event.contact().getPublicId()
                + " | lead:" + event.originLead().getPublicId();
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_CREATED_FROM_LEAD, true, details);
        log.debug("CONTACT_CREATED_FROM_LEAD logged — contact: {}, lead: {}",
                event.contact().getPublicId(), event.originLead().getPublicId());
    }

    // =========================================================================
    // Status
    // =========================================================================

    /**
     * Persists a {@link ContactAction#CONTACT_STATUS_CHANGED} entry when the
     * contact's CRM status transitions.
     *
     * @param event contains the contact, previous status, new status, actor and device
     */
    public void logStatusChanged(ContactStatusChangedEvent event) {
        String details = "publicId:" + event.contact().getPublicId()
                + " | status: " + event.previousStatus().name() + " → " + event.newStatus().name();
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_STATUS_CHANGED, true, details);
        log.debug("CONTACT_STATUS_CHANGED logged — contact: {}, {} → {}",
                event.contact().getPublicId(), event.previousStatus(), event.newStatus());
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Persists a {@link ContactAction#CONTACT_ASSIGNED} entry when a contact
     * is assigned or unassigned from a commercial.
     *
     * @param event contains the contact, new/previous assignee, actor and device
     */
    public void logAssigned(ContactAssignedEvent event) {
        String assigneeName = event.newAssignee() != null ? event.newAssignee().getUsername() : "none";
        String details = "publicId:" + event.contact().getPublicId()
                + " | assignee: " + assigneeName;
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_ASSIGNED, true, details);
        log.debug("CONTACT_ASSIGNED logged — contact: {}, assignee: {}",
                event.contact().getPublicId(), assigneeName);
    }

    // =========================================================================
    // Merge
    // =========================================================================

    /**
     * Persists a {@link ContactAction#CONTACT_MERGED} entry on the surviving
     * (target) contact when two contacts are merged.
     *
     * @param event contains target contact, source contact, actor and device
     */
    public void logMerged(ContactMergedEvent event) {
        String details = "publicId:" + event.targetContact().getPublicId()
                + " | merged from: " + event.sourceContact().getPublicId();
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_MERGED, true, details);
        log.debug("CONTACT_MERGED logged — target: {}, source: {}",
                event.targetContact().getPublicId(), event.sourceContact().getPublicId());
    }

    // =========================================================================
    // Organisation
    // =========================================================================

    /**
     * Persists a {@link ContactAction#CONTACT_ORG_LINKED} entry when a contact
     * is linked to an organisation.
     *
     * @param event contains the contact, organisation, actor and device
     */
    public void logOrgLinked(ContactLinkedToOrganisationEvent event) {
        String details = "publicId:" + event.contact().getPublicId()
                + " | org: " + event.organisation().getName();
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_ORG_LINKED, true, details);
        log.debug("CONTACT_ORG_LINKED logged — contact: {}, org: {}",
                event.contact().getPublicId(), event.organisation().getName());
    }

    /**
     * Persists a {@link ContactAction#CONTACT_ORG_UNLINKED} entry when a contact
     * is unlinked from its organisation.
     *
     * @param event contains the contact, previous organisation, actor and device
     */
    public void logOrgUnlinked(ContactUnlinkedFromOrganisationEvent event) {
        String details = "publicId:" + event.contact().getPublicId()
                + " | was linked to: " + event.previousOrg().getName();
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_ORG_UNLINKED, true, details);
        log.debug("CONTACT_ORG_UNLINKED logged — contact: {}, org: {}",
                event.contact().getPublicId(), event.previousOrg().getName());
    }

    // =========================================================================
    // Archive
    // =========================================================================

    /**
     * Persists a {@link ContactAction#CONTACT_ARCHIVED} entry when a contact
     * is archived (soft deletion).
     *
     * @param event contains the contact, actor and device
     */
    public void logArchived(ContactArchivedEvent event) {
        String details = "publicId:" + event.contact().getPublicId();
        logUserActivity(event.actor(), event.actorDevice(), ContactAction.CONTACT_ARCHIVED, true, details);
        log.debug("CONTACT_ARCHIVED logged — contact: {}", event.contact().getPublicId());
    }
}
