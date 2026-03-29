package be.steby.CoreProject.bll.domains.organisation.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.organisation.events.*;
import be.steby.CoreProject.bll.domains.organisation.listeners.OrganisationActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.OrganisationAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CRM organisation domain activity log service.
 *
 * <p>Translates organisation domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to an {@link OrganisationAction} constant.</p>
 *
 * <p>Called exclusively from {@link OrganisationActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 *
 * <p>Note: {@link be.steby.CoreProject.bll.domains.organisation.events.OrganisationUpdatedEvent}
 * is intentionally not handled here — field-level changes are already tracked by
 * {@link OrganisationChangeLogService}.</p>
 *
 * <p>{@code actionDetails} always begins with {@code "publicId:<value>"} so that
 * per-organisation queries can be performed via a LIKE filter on the existing
 * {@link be.steby.CoreProject.dl.entities.ActivityLog#getActionDetails()} column.</p>
 */
@Service
@Slf4j
public class OrganisationActivityLogService extends ActivityLogService {

    public OrganisationActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "CRM_ORGANISATION";
    }

    // =========================================================================
    // Creation
    // =========================================================================

    /**
     * Persists an {@link OrganisationAction#ORGANISATION_CREATED} entry when
     * a new organisation is created.
     *
     * @param event contains the organisation, actor and device
     */
    public void logCreated(OrganisationCreatedEvent event) {
        String details = "publicId:" + event.organisation().getPublicId()
                + " | name: " + event.organisation().getName();
        logUserActivity(event.actor(), event.actorDevice(), OrganisationAction.ORGANISATION_CREATED, true, details);
        log.debug("ORGANISATION_CREATED logged — org: {}", event.organisation().getPublicId());
    }

    // =========================================================================
    // Archive
    // =========================================================================

    /**
     * Persists an {@link OrganisationAction#ORGANISATION_ARCHIVED} entry when
     * an organisation is archived (soft deletion, typically after a merge).
     *
     * @param event contains the organisation, actor and device
     */
    public void logArchived(OrganisationArchivedEvent event) {
        String details = "publicId:" + event.organisation().getPublicId()
                + " | name: " + event.organisation().getName();
        logUserActivity(event.actor(), event.actorDevice(), OrganisationAction.ORGANISATION_ARCHIVED, true, details);
        log.debug("ORGANISATION_ARCHIVED logged — org: {}", event.organisation().getPublicId());
    }

    // =========================================================================
    // Merge
    // =========================================================================

    /**
     * Persists an {@link OrganisationAction#ORGANISATION_MERGED} entry on the
     * surviving (target) organisation when two organisations are merged.
     *
     * @param event contains target organisation, source organisation, actor and device
     */
    public void logMerged(OrganisationMergedEvent event) {
        String details = "publicId:" + event.targetOrganisation().getPublicId()
                + " | merged from: " + event.sourceOrganisation().getPublicId();
        logUserActivity(event.actor(), event.actorDevice(), OrganisationAction.ORGANISATION_MERGED, true, details);
        log.debug("ORGANISATION_MERGED logged — target: {}, source: {}",
                event.targetOrganisation().getPublicId(), event.sourceOrganisation().getPublicId());
    }
}
