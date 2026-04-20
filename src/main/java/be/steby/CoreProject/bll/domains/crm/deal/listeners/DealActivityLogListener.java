package be.steby.CoreProject.bll.domains.crm.deal.listeners;

import be.steby.CoreProject.bll.domains.crm.deal.events.*;
import be.steby.CoreProject.bll.domains.crm.deal.services.DealChangeLogService;
import be.steby.CoreProject.bll.domains.crm.deal.services.DealActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for CRM deal activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link DealActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after notification listeners
 * ({@code @Order(10)}) — a logging failure never blocks email delivery.</p>
 *
 * <p>Note: {@link DealUpdatedEvent} is intentionally not handled here —
 * field-level changes are already tracked by
 * {@link DealChangeLogService}.</p>
 *
 * Covered events → DealAction mapping:
 * <ul>
 *   <li>{@link DealCreatedEvent}      → DEAL_CREATED</li>
 *   <li>{@link DealStageChangedEvent} → DEAL_STAGE_MOVED</li>
 *   <li>{@link DealWonEvent}          → DEAL_WON</li>
 *   <li>{@link DealLostEvent}         → DEAL_LOST</li>
 *   <li>{@link DealReassignedEvent}   → DEAL_REASSIGNED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DealActivityLogListener {

    private final DealActivityLogService dealActivityLogService;

    // =========================================================================
    // Creation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleDealCreated(DealCreatedEvent event) {
        try {
            log.debug("Processing deal created event — deal: {}", event.deal().getPublicId());
            dealActivityLogService.logCreated(event);
        } catch (Exception e) {
            log.error("Failed to log deal creation — deal: {}", event.deal().getPublicId(), e);
        }
    }

    // =========================================================================
    // Stage
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleDealStageChanged(DealStageChangedEvent event) {
        try {
            log.debug("Processing deal stage changed event — deal: {}", event.deal().getPublicId());
            dealActivityLogService.logStageMoved(event);
        } catch (Exception e) {
            log.error("Failed to log deal stage move — deal: {}", event.deal().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleDealWon(DealWonEvent event) {
        try {
            log.debug("Processing deal won event — deal: {}", event.deal().getPublicId());
            dealActivityLogService.logWon(event);
        } catch (Exception e) {
            log.error("Failed to log deal won — deal: {}", event.deal().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleDealLost(DealLostEvent event) {
        try {
            log.debug("Processing deal lost event — deal: {}", event.deal().getPublicId());
            dealActivityLogService.logLost(event);
        } catch (Exception e) {
            log.error("Failed to log deal lost — deal: {}", event.deal().getPublicId(), e);
        }
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleDealReassigned(DealReassignedEvent event) {
        try {
            log.debug("Processing deal reassigned event — deal: {}", event.deal().getPublicId());
            dealActivityLogService.logReassigned(event);
        } catch (Exception e) {
            log.error("Failed to log deal reassignment — deal: {}", event.deal().getPublicId(), e);
        }
    }
}
