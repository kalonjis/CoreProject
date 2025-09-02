package be.steby.CoreProject.bll.common.services.activitylog;

import be.steby.CoreProject.bll.common.events.UserActionEvent;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.ActionLogType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Abstract base class for domain-specific activity log services
 * Version KISS - Focus on event-driven approach and simplicity
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractActivityLogService {

    protected final ActivityLogRepository activityLogRepository;
    protected final ApplicationEventPublisher eventPublisher;

    /**
     * Domain name (AUTH, PASSWORD, EMAIL, ACCOUNT, ADMIN, DEVICE)
     * Must be implemented by each domain service
     */
    protected abstract String getDomainName();

    // ================== MAIN EVENT-DRIVEN METHODS ==================

    /**
     * Publish a user action event for asynchronous logging
     */
    protected void publishActionEvent(User user, Device device, ActionLogType actionType,
                                      boolean successful, String details) {
        UserActionEvent event = UserActionEvent.success(user, device, actionType, details);
        if (!successful) {
            event = new UserActionEvent(user, device, actionType, successful, details);
        }

        eventPublisher.publishEvent(event);

        log.debug("Published {} event for user {} - action: {}",
                getDomainName(), user.getUsername(), actionType);
    }

    /**
     * Publish action event with RequestContext for enrichment
     */
    protected void publishActionEvent(User user, Device device, ActionLogType actionType,
                                      boolean successful, String details, RequestContext context) {
        UserActionEvent event = new UserActionEvent(user, device, actionType, successful, details, context);
        eventPublisher.publishEvent(event);

        log.debug("Published {} event for user {} - action: {} from IP: {}",
                getDomainName(), user.getUsername(), actionType,
                context != null ? context.getClientIp() : "unknown");
    }

    /**
     * Publish failure event with specific failure reason
     */
    protected void publishFailureEvent(User user, Device device, ActionLogType actionType,
                                       String details, String failureReason) {
        UserActionEvent event = UserActionEvent.failure(user, device, actionType, details, failureReason);
        eventPublisher.publishEvent(event);

        log.debug("Published {} failure event for user {} - action: {} - reason: {}",
                getDomainName(), user.getUsername(), actionType, failureReason);
    }

    /**
     * Publish failure event with context
     */
    protected void publishFailureEvent(User user, Device device, ActionLogType actionType,
                                       String details, String failureReason, RequestContext context) {
        UserActionEvent event = new UserActionEvent(user, device, actionType, false, details, failureReason, context);
        eventPublisher.publishEvent(event);

        log.debug("Published {} failure event for user {} - action: {} - reason: {}",
                getDomainName(), user.getUsername(), actionType, failureReason);
    }

    // ================== COMMON QUERY METHODS ==================

    /**
     * Get activity history for this domain and user
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserDomainHistory(User user, Pageable pageable) {
        return activityLogRepository.findByUserAndActionCategoryOrderByTimestampDesc(
                user, getDomainName(), pageable
        );
    }

    /**
     * Get recent domain actions for a user (last X days)
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getRecentDomainActions(User user, int days, Pageable pageable) {
        Instant since = Instant.now().minusSeconds(days * 24L * 3600L);
        return activityLogRepository.findByUserAndActionCategoryAndTimestampAfterOrderByTimestampDesc(
                user, getDomainName(), since, pageable
        );
    }

    /**
     * Get failed domain actions for a user
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getFailedDomainActions(User user, Pageable pageable) {
        return activityLogRepository.findByUserAndActionCategoryAndSuccessfulOrderByTimestampDesc(
                user, getDomainName(), false, pageable
        );
    }

    /**
     * Get successful domain actions for a user
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getSuccessfulDomainActions(User user, Pageable pageable) {
        return activityLogRepository.findByUserAndActionCategoryAndSuccessfulOrderByTimestampDesc(
                user, getDomainName(), true, pageable
        );
    }

    /**
     * Get activities by specific device in this domain
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getDomainActivitiesByDevice(Device device, Pageable pageable) {
        // Note: This would need a custom query method in repository
        // For now, using basic device query - can be enhanced later
        return activityLogRepository.findByDeviceOrderByTimestampDesc(device, pageable);
    }

    // ================== UTILITY METHODS ==================

    /**
     * Count actions of a specific type for user in last X days
     */
    protected long countActionType(User user, ActionLogType actionType, int days) {
        Instant since = Instant.now().minusSeconds(days * 24L * 3600L);
        return activityLogRepository.countByUserAndActionTypeAndTimestampAfter(
                user, actionType.getName(), since
        );
    }

    /**
     * Check if user performed specific action recently (anti-spam protection)
     */
    protected boolean hasRecentAction(User user, ActionLogType actionType, int minutesThreshold) {
        Instant threshold = Instant.now().minusSeconds(minutesThreshold * 60L);
        return activityLogRepository.existsByUserAndActionTypeAndTimestampAfter(
                user, actionType.getName(), threshold
        );
    }

    /**
     * Count successful actions in domain for user in last X days
     */
    protected long countSuccessfulDomainActions(User user, int days) {
        Instant since = Instant.now().minusSeconds(days * 24L * 3600L);
        return activityLogRepository.countByUserAndActionCategoryAndSuccessfulAndTimestampAfter(
                user, getDomainName(), true, since
        );
    }

    /**
     * Count failed actions in domain for user in last X days
     */
    protected long countFailedDomainActions(User user, int days) {
        Instant since = Instant.now().minusSeconds(days * 24L * 3600L);
        return activityLogRepository.countByUserAndActionCategoryAndSuccessfulAndTimestampAfter(
                user, getDomainName(), false, since
        );
    }

    /**
     * Check if user has had any failed actions in this domain recently
     */
    protected boolean hasRecentFailures(User user, int hours) {
        Instant threshold = Instant.now().minusSeconds(hours * 3600L);
        // Use category-based check instead of specific action type
        return activityLogRepository.countByUserAndActionCategoryAndSuccessfulAndTimestampAfter(
                user, getDomainName(), false, threshold
        ) > 0;
    }

    // ================== VALIDATION HELPERS ==================

    /**
     * Validate that required parameters are not null
     */
    protected void validateRequiredParams(User user, ActionLogType actionType) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (actionType == null) {
            throw new IllegalArgumentException("ActionLogType cannot be null");
        }
    }

    /**
     * Validate that the action type belongs to this domain
     */
    protected void validateDomainAction(ActionLogType actionType) {
        if (!getDomainName().equals(actionType.getCategory())) {
            throw new IllegalArgumentException(
                    String.format("ActionType %s (category: %s) does not belong to domain %s",
                            actionType, actionType.getCategory(), getDomainName())
            );
        }
    }
}