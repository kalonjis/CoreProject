package be.steby.CoreProject.bll.domains.crm.organisation.listeners;

import be.steby.CoreProject.bll.domains.crm.organisation.events.OrganisationArchivedEvent;
import be.steby.CoreProject.bll.domains.crm.organisation.events.OrganisationCreatedEvent;
import be.steby.CoreProject.bll.domains.crm.organisation.events.OrganisationMergedEvent;
import be.steby.CoreProject.bll.domains.crm.organisation.events.OrganisationUpdatedEvent;
import be.steby.CoreProject.bll.domains.crm.organisation.services.OrganisationChangeLogService;
import be.steby.CoreProject.bll.domains.crm.organisation.services.OrganisationActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for CRM organisation activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link OrganisationActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after notification listeners
 * ({@code @Order(10)}) — a logging failure never blocks email delivery.</p>
 *
 * <p>Note: {@link OrganisationUpdatedEvent} is intentionally not handled here —
 * field-level changes are already tracked by
 * {@link OrganisationChangeLogService}.</p>
 *
 * Covered events → OrganisationAction mapping:
 * <ul>
 *   <li>{@link OrganisationCreatedEvent}  → ORGANISATION_CREATED</li>
 *   <li>{@link OrganisationArchivedEvent} → ORGANISATION_ARCHIVED</li>
 *   <li>{@link OrganisationMergedEvent}   → ORGANISATION_MERGED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class OrganisationActivityLogListener {

    private final OrganisationActivityLogService organisationActivityLogService;

    // =========================================================================
    // Creation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleOrganisationCreated(OrganisationCreatedEvent event) {
        try {
            log.debug("Processing organisation created event — org: {}", event.organisation().getPublicId());
            organisationActivityLogService.logCreated(event);
        } catch (Exception e) {
            log.error("Failed to log organisation creation — org: {}", event.organisation().getPublicId(), e);
        }
    }

    // =========================================================================
    // Archive
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleOrganisationArchived(OrganisationArchivedEvent event) {
        try {
            log.debug("Processing organisation archived event — org: {}", event.organisation().getPublicId());
            organisationActivityLogService.logArchived(event);
        } catch (Exception e) {
            log.error("Failed to log organisation archival — org: {}", event.organisation().getPublicId(), e);
        }
    }

    // =========================================================================
    // Merge
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleOrganisationMerged(OrganisationMergedEvent event) {
        try {
            log.debug("Processing organisation merged event — target: {}", event.targetOrganisation().getPublicId());
            organisationActivityLogService.logMerged(event);
        } catch (Exception e) {
            log.error("Failed to log organisation merge — target: {}", event.targetOrganisation().getPublicId(), e);
        }
    }
}
