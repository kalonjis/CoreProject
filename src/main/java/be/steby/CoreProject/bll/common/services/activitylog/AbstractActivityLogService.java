package be.steby.CoreProject.bll.common.services.activitylog;

import be.steby.CoreProject.bll.common.events.UserActionEvent;
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

/**
 * Simplified abstract base class for domain-specific activity log services.
 * Focuses on event-driven approach without RequestContext complexity.
 *
 * All device detection is now handled at the service layer before event publishing,
 * making this class much simpler and more focused.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractActivityLogService {

    protected final ActivityLogRepository activityLogRepository;
    protected final ApplicationEventPublisher eventPublisher;

    /**
     * Domain name for logging (AUTH, PASSWORD, EMAIL, ACCOUNT, ADMIN, DEVICE)
     * Must be implemented by each domain service
     */
    protected abstract String getDomainName();

    // ================== SIMPLIFIED EVENT PUBLISHING METHODS ==================

    /**
     * Publish a successful user action event
     * @param user The user performing the action
     * @param device The device used (may be null)
     * @param actionType The type of action
     * @param details Additional details
     */
    protected void publishSuccessEvent(User user, Device device, ActionLogType actionType, String details) {
        UserActionEvent event = new UserActionEvent(user, device, actionType, true, details);
        eventPublisher.publishEvent(event);

        log.debug("Published {} success event for user {} - action: {} - device: {}",
                getDomainName(), user.getUsername(), actionType,
                device != null ? device.getId() : "unknown");
    }

    /**
     * Publish a failed user action event
     * @param user The user performing the action
     * @param device The device used (may be null)
     * @param actionType The type of action
     * @param details Additional details
     * @param failureReason The reason for failure
     */
    protected void publishFailureEvent(User user, Device device, ActionLogType actionType,
                                       String details, String failureReason) {
        UserActionEvent event = new UserActionEvent(user, device, actionType, false, details, failureReason);
        eventPublisher.publishEvent(event);

        log.debug("Published {} failure event for user {} - action: {} - reason: {} - device: {}",
                getDomainName(), user.getUsername(), actionType, failureReason,
                device != null ? device.getId() : "unknown");
    }

    /**
     * Publish a generic user action event (success status determined by caller)
     * @param user The user performing the action
     * @param device The device used (may be null)
     * @param actionType The type of action
     * @param successful Whether the action was successful
     * @param details Additional details
     */
    protected void publishActionEvent(User user, Device device, ActionLogType actionType,
                                      boolean successful, String details) {
        if (successful) {
            publishSuccessEvent(user, device, actionType, details);
        } else {
            publishFailureEvent(user, device, actionType, details, null);
        }
    }

    /**
     * Publish action event with failure reason
     * @param user The user performing the action
     * @param device The device used (may be null)
     * @param actionType The type of action
     * @param successful Whether the action was successful
     * @param details Additional details
     * @param failureReason The reason for failure (only used if successful = false)
     */
    protected void publishActionEvent(User user, Device device, ActionLogType actionType,
                                      boolean successful, String details, String failureReason) {
        if (successful) {
            publishSuccessEvent(user, device, actionType, details);
        } else {
            publishFailureEvent(user, device, actionType, details, failureReason);
        }
    }

    // ================== COMMON QUERY METHODS ==================

    /**
     * Get activity history for this domain and user
     * @param user The user to get history for
     * @param pageable Pagination parameters
     * @return Paginated activity logs
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserDomainHistory(User user, Pageable pageable) {
        // Note: This assumes you have a method to filter by action category/domain
        // You might need to adapt this based on your ActivityLogRepository implementation
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    /**
     * Get activity history for a specific device
     * @param device The device to get history for
     * @param pageable Pagination parameters
     * @return Paginated activity logs
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getDeviceDomainHistory(Device device, Pageable pageable) {
        return activityLogRepository.findByDeviceOrderByTimestampDesc(device, pageable);
    }

    /**
     * Get recent activities for a user
     * @param user The user
     * @param limit Maximum number of results
     * @return Recent activity logs
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getRecentUserActivity(User user, Pageable pageable) {
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    // ================== DOMAIN-SPECIFIC HELPER METHODS ==================
    // These can be overridden by specific domain services if needed

    /**
     * Log a successful action directly (bypasses event system)
     * Use this for special cases where immediate logging is required
     * @param user The user
     * @param device The device (may be null)
     * @param actionType The action type
     * @param details Additional details
     * @return The saved activity log
     */
    @Transactional
    protected ActivityLog logSuccessfulActionDirectly(User user, Device device, ActionLogType actionType, String details) {
        ActivityLog activityLog = actionType.createActivityLog(user, true);

        if (device != null) {
            activityLog.setDevice(device);
        }

        if (details != null && !details.trim().isEmpty()) {
            activityLog.setDetails(details.trim());
        }

        ActivityLog savedLog = activityLogRepository.save(activityLog);

        log.debug("Directly logged {} action for user {} - ID: {}",
                actionType, user.getUsername(), savedLog.getId());

        return savedLog;
    }

    /**
     * Log a failed action directly (bypasses event system)
     * @param user The user
     * @param device The device (may be null)
     * @param actionType The action type
     * @param details Additional details
     * @param failureReason The failure reason
     * @return The saved activity log
     */
    @Transactional
    protected ActivityLog logFailedActionDirectly(User user, Device device, ActionLogType actionType,
                                                  String details, String failureReason) {
        ActivityLog activityLog = actionType.createActivityLog(user, false);

        if (device != null) {
            activityLog.setDevice(device);
        }

        if (details != null && !details.trim().isEmpty()) {
            activityLog.setDetails(details.trim());
        }

        if (failureReason != null && !failureReason.trim().isEmpty()) {
            activityLog.setFailureReason(failureReason.trim());
        }

        ActivityLog savedLog = activityLogRepository.save(activityLog);

        log.debug("Directly logged failed {} action for user {} - ID: {}",
                actionType, user.getUsername(), savedLog.getId());

        return savedLog;
    }
}