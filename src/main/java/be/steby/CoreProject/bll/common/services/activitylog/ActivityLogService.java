package be.steby.CoreProject.bll.common.services.activitylog;

import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.ActionLogType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic activity log service - DRY principle
 * Base service for all domain-specific activity log services
 */
@RequiredArgsConstructor
@Slf4j
public abstract class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Generic method to log user activity asynchronously
     * @param user The user performing the action
     * @param device The device used (can be null)
     * @param actionLogType The type of action
     * @param successful Whether the action was successful
     */
    @Async("activityLogExecutor")
    @Transactional
    public void logUserActivity(User user, Device device, ActionLogType actionLogType, boolean successful) {
        logUserActivity(user, device, actionLogType, successful, null);
    }

    /**
     * Generic method to log user activity asynchronously with failure reason
     * @param user The user performing the action
     * @param device The device used (can be null)
     * @param actionLogType The type of action
     * @param successful Whether the action was successful
     * @param failureReason Reason for failure (if any)
     */
    @Async("activityLogExecutor")
    @Transactional
    public void logUserActivity(User user, Device device, ActionLogType actionLogType, boolean successful, String failureReason) {
        try {
            ActivityLog activityLog = actionLogType.createActivityLog(user, successful)
                    .toBuilder()
                    .device(device)
                    .failureReason(failureReason)
                    .build();

            activityLogRepository.save(activityLog);

            log.debug("Activity logged: user={}, action={}, successful={}",
                    user.getUsername(), actionLogType.getName(), successful);
        } catch (Exception e) {
            log.error("Failed to log activity for user: {}, action: {}",
                    user.getUsername(), actionLogType.getName(), e);
        }
    }

    /**
     * Get domain name for logging context
     * To be implemented by domain-specific services
     */
    protected abstract String getDomainName();
}