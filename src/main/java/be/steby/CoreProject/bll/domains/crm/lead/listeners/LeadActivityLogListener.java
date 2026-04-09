package be.steby.CoreProject.bll.domains.crm.lead.listeners;

import be.steby.CoreProject.bll.domains.crm.lead.events.*;
import be.steby.CoreProject.bll.domains.crm.lead.services.LeadActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for CRM lead activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link LeadActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after notification listeners
 * ({@code @Order(10)}) — a logging failure never blocks email delivery.</p>
 *
 * Covered events → LeadAction mapping:
 * <ul>
 *   <li>{@link LeadSubmittedEvent}  → LEAD_SUBMITTED</li>
 *   <li>{@link LeadAssignedEvent}   → LEAD_ASSIGNED</li>
 *   <li>{@link LeadInReviewEvent}   → LEAD_IN_REVIEW</li>
 *   <li>{@link LeadConvertedEvent}  → LEAD_CONVERTED</li>
 *   <li>{@link LeadRejectedEvent}   → LEAD_REJECTED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class LeadActivityLogListener {

    private final LeadActivityLogService leadActivityLogService;

    // =========================================================================
    // Submission
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleLeadSubmitted(LeadSubmittedEvent event) {
        try {
            log.debug("Processing lead submitted event — lead: {}", event.lead().getPublicId());
            leadActivityLogService.logSubmitted(event);
        } catch (Exception e) {
            log.error("Failed to log lead submission — lead: {}", event.lead().getPublicId(), e);
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleLeadAssigned(LeadAssignedEvent event) {
        try {
            log.debug("Processing lead assigned event — lead: {}", event.lead().getPublicId());
            leadActivityLogService.logAssigned(event);
        } catch (Exception e) {
            log.error("Failed to log lead assignment — lead: {}", event.lead().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleLeadInReview(LeadInReviewEvent event) {
        try {
            log.debug("Processing lead in review event — lead: {}", event.lead().getPublicId());
            leadActivityLogService.logInReview(event);
        } catch (Exception e) {
            log.error("Failed to log lead in review — lead: {}", event.lead().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleLeadConverted(LeadConvertedEvent event) {
        try {
            log.debug("Processing lead converted event — lead: {}", event.lead().getPublicId());
            leadActivityLogService.logConverted(event);
        } catch (Exception e) {
            log.error("Failed to log lead conversion — lead: {}", event.lead().getPublicId(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleLeadRejected(LeadRejectedEvent event) {
        try {
            log.debug("Processing lead rejected event — lead: {}", event.lead().getPublicId());
            leadActivityLogService.logRejected(event);
        } catch (Exception e) {
            log.error("Failed to log lead rejection — lead: {}", event.lead().getPublicId(), e);
        }
    }
}
