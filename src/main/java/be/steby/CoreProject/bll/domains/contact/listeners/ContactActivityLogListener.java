package be.steby.CoreProject.bll.domains.contact.listeners;

import be.steby.CoreProject.bll.domains.contact.events.*;
import be.steby.CoreProject.bll.domains.contact.services.ContactActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for CRM contact activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link ContactActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after notification listeners
 * ({@code @Order(10)}) — a logging failure never blocks email delivery.</p>
 *
 * <p>Note: {@link be.steby.CoreProject.bll.domains.contact.events.ContactUpdatedEvent}
 * is intentionally not handled here — field-level changes are already tracked by
 * {@link be.steby.CoreProject.bll.domains.contact.services.ContactChangeLogService}.</p>
 *
 * Covered events → ContactAction mapping:
 * <ul>
 *   <li>{@link ContactCreatedEvent}                  → CONTACT_CREATED</li>
 *   <li>{@link ContactCreatedFromLeadEvent}          → CONTACT_CREATED_FROM_LEAD</li>
 *   <li>{@link ContactStatusChangedEvent}            → CONTACT_STATUS_CHANGED</li>
 *   <li>{@link ContactAssignedEvent}                 → CONTACT_ASSIGNED</li>
 *   <li>{@link ContactMergedEvent}                   → CONTACT_MERGED</li>
 *   <li>{@link ContactLinkedToOrganisationEvent}     → CONTACT_ORG_LINKED</li>
 *   <li>{@link ContactUnlinkedFromOrganisationEvent} → CONTACT_ORG_UNLINKED</li>
 *   <li>{@link ContactArchivedEvent}                 → CONTACT_ARCHIVED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class ContactActivityLogListener {

    private final ContactActivityLogService contactActivityLogService;

    // =========================================================================
    // Creation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactCreated(ContactCreatedEvent event) {
        try {
            log.debug("Processing contact created event — contact: {}", event.contact().getPublicId());
            contactActivityLogService.logCreated(event);
        } catch (Exception e) {
            log.error("Failed to log contact creation — contact: {}", event.contact().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactCreatedFromLead(ContactCreatedFromLeadEvent event) {
        try {
            log.debug("Processing contact created from lead event — contact: {}", event.contact().getPublicId());
            contactActivityLogService.logCreatedFromLead(event);
        } catch (Exception e) {
            log.error("Failed to log contact creation from lead — contact: {}", event.contact().getPublicId(), e);
        }
    }

    // =========================================================================
    // Status
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactStatusChanged(ContactStatusChangedEvent event) {
        try {
            log.debug("Processing contact status changed event — contact: {}", event.contact().getPublicId());
            contactActivityLogService.logStatusChanged(event);
        } catch (Exception e) {
            log.error("Failed to log contact status change — contact: {}", event.contact().getPublicId(), e);
        }
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactAssigned(ContactAssignedEvent event) {
        try {
            log.debug("Processing contact assigned event — contact: {}", event.contact().getPublicId());
            contactActivityLogService.logAssigned(event);
        } catch (Exception e) {
            log.error("Failed to log contact assignment — contact: {}", event.contact().getPublicId(), e);
        }
    }

    // =========================================================================
    // Merge
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactMerged(ContactMergedEvent event) {
        try {
            log.debug("Processing contact merged event — target: {}", event.targetContact().getPublicId());
            contactActivityLogService.logMerged(event);
        } catch (Exception e) {
            log.error("Failed to log contact merge — target: {}", event.targetContact().getPublicId(), e);
        }
    }

    // =========================================================================
    // Organisation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactLinkedToOrganisation(ContactLinkedToOrganisationEvent event) {
        try {
            log.debug("Processing contact linked to org event — contact: {}", event.contact().getPublicId());
            contactActivityLogService.logOrgLinked(event);
        } catch (Exception e) {
            log.error("Failed to log contact org link — contact: {}", event.contact().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactUnlinkedFromOrganisation(ContactUnlinkedFromOrganisationEvent event) {
        try {
            log.debug("Processing contact unlinked from org event — contact: {}", event.contact().getPublicId());
            contactActivityLogService.logOrgUnlinked(event);
        } catch (Exception e) {
            log.error("Failed to log contact org unlink — contact: {}", event.contact().getPublicId(), e);
        }
    }

    // =========================================================================
    // Archive
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleContactArchived(ContactArchivedEvent event) {
        try {
            log.debug("Processing contact archived event — contact: {}", event.contact().getPublicId());
            contactActivityLogService.logArchived(event);
        } catch (Exception e) {
            log.error("Failed to log contact archival — contact: {}", event.contact().getPublicId(), e);
        }
    }
}
