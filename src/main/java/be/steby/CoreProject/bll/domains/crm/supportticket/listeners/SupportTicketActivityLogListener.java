package be.steby.CoreProject.bll.domains.crm.supportticket.listeners;

import be.steby.CoreProject.bll.domains.crm.supportticket.events.*;
import be.steby.CoreProject.bll.domains.crm.supportticket.services.SupportTicketActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for CRM support ticket activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link SupportTicketActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after notification listeners
 * ({@code @Order(10)}) — a logging failure never blocks email delivery.</p>
 *
 * <p>Note: {@link SupportTicketUpdatedEvent} is intentionally not handled here —
 * content edits (subject, description) are minor operations that do not warrant
 * a dedicated activity log entry.</p>
 *
 * Covered events → SupportTicketAction mapping:
 * <ul>
 *   <li>{@link SupportTicketCreatedEvent}       → TICKET_CREATED</li>
 *   <li>{@link SupportTicketStatusChangedEvent} → TICKET_STATUS_CHANGED</li>
 *   <li>{@link SupportTicketClosedEvent}        → TICKET_CLOSED</li>
 *   <li>{@link SupportTicketAssignedEvent}      → TICKET_ASSIGNED</li>
 *   <li>{@link SupportTicketDeletedEvent}       → TICKET_DELETED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class SupportTicketActivityLogListener {

    private final SupportTicketActivityLogService supportTicketActivityLogService;

    // =========================================================================
    // Creation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleSupportTicketCreated(SupportTicketCreatedEvent event) {
        try {
            log.debug("Processing support ticket created event — ticket: {}", event.ticket().getPublicId());
            supportTicketActivityLogService.logCreated(event);
        } catch (Exception e) {
            log.error("Failed to log support ticket creation — ticket: {}", event.ticket().getPublicId(), e);
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleSupportTicketStatusChanged(SupportTicketStatusChangedEvent event) {
        try {
            log.debug("Processing support ticket status changed event — ticket: {}", event.ticket().getPublicId());
            supportTicketActivityLogService.logStatusChanged(event);
        } catch (Exception e) {
            log.error("Failed to log support ticket status change — ticket: {}", event.ticket().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleSupportTicketClosed(SupportTicketClosedEvent event) {
        try {
            log.debug("Processing support ticket closed event — ticket: {}", event.ticket().getPublicId());
            supportTicketActivityLogService.logClosed(event);
        } catch (Exception e) {
            log.error("Failed to log support ticket closure — ticket: {}", event.ticket().getPublicId(), e);
        }
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleSupportTicketAssigned(SupportTicketAssignedEvent event) {
        try {
            log.debug("Processing support ticket assigned event — ticket: {}", event.ticket().getPublicId());
            supportTicketActivityLogService.logAssigned(event);
        } catch (Exception e) {
            log.error("Failed to log support ticket assignment — ticket: {}", event.ticket().getPublicId(), e);
        }
    }

    // =========================================================================
    // Deletion
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleSupportTicketDeleted(SupportTicketDeletedEvent event) {
        try {
            log.debug("Processing support ticket deleted event — ticket: {}", event.ticket().getPublicId());
            supportTicketActivityLogService.logDeleted(event);
        } catch (Exception e) {
            log.error("Failed to log support ticket deletion — ticket: {}", event.ticket().getPublicId(), e);
        }
    }
}
