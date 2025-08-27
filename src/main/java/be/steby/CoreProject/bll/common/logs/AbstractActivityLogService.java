package be.steby.CoreProject.bll.common.logs;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.ActionLogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract base class providing core activity logging functionality.
 * KISS principle: Simple, focused, and delegates complex logic to specialized services.
 *
 * @author Steby Core Project Team
 * @version 2.0 - KISS Edition
 */
@Slf4j
public abstract class AbstractActivityLogService {

    // ================== DEPENDENCIES ==================

    protected final ActivityLogRepository activityLogRepository;

    // ================== CONSTRUCTOR ==================

    protected AbstractActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    // ================== ABSTRACT METHODS ==================

    /**
     * Returns the domain name for this service (e.g., "AUTH", "ADMIN", "ACCOUNT")
     */
    protected abstract String getDomainName();

    // ================== CORE LOGGING METHODS ==================

    /**
     * Main method for logging user actions - SIMPLE and focused
     */
    @Transactional
    public ActivityLog logUserAction(User user, Device device, ActionLogType actionType,
                                     boolean successful, String details,
                                     Map<String, Object> additionalData, RequestContext context) {

        log.debug("Logging {} action for user {}: {}",
                getDomainName(), user.getUsername(), ((Enum<?>) actionType).name());

        // Create activity log using ActionLogType factory method (leverages your existing system)
        ActivityLog activityLog = actionType.createActivityLogWithContext(user, successful, context);

        // Simple enrichment
        if (device != null) {
            activityLog.setDevice(device);
        }

        if (details != null) {
            activityLog.setActionDetails(details);
        }

        // Save and return
        return activityLogRepository.save(activityLog);
    }

    /**
     * Simplified logging for successful actions
     */
    @Transactional
    public ActivityLog logSuccess(User user, Device device, ActionLogType actionType, RequestContext context) {
        return logUserAction(user, device, actionType, true, null, null, context);
    }

    /**
     * Simplified logging for failed actions
     */
    @Transactional
    public ActivityLog logFailure(User user, Device device, ActionLogType actionType,
                                  String failureReason, RequestContext context) {
        ActivityLog activityLog = logUserAction(user, device, actionType, false, failureReason, null, context);
        activityLog.setFailureReason(failureReason);
        return activityLogRepository.save(activityLog);
    }

    // ================== QUERY METHODS ==================

    /**
     * Gets recent user activity - simple pagination
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getRecentUserActivity(User user, Pageable pageable) {
        List<ActivityLog> allLogs = activityLogRepository.findByUserOrderByTimestampDesc(user);
        return convertToPage(allLogs, pageable);
    }

    /**
     * Gets logs by action type
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getLogsByActionType(ActionLogType actionType, int limit) {
        return activityLogRepository.findByActionTypeOrderByTimestampDesc(actionType)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets the most recent log for a user and action type
     */
    @Transactional(readOnly = true)
    public Optional<ActivityLog> getLatestUserLog(User user, ActionLogType actionType) {
        return activityLogRepository.findTopByUserAndActionTypeOrderByTimestampDesc(user, actionType);
    }

    /**
     * Counts failed attempts within a time period - for basic security
     */
    @Transactional(readOnly = true)
    public long countRecentFailures(User user, ActionLogType actionType, Duration period) {
        Instant since = Instant.now().minus(period);
        return activityLogRepository.countByActionTypeAndSuccessfulAndTimestampBetween(
                actionType, false, since, Instant.now());
    }

    // ================== UTILITY METHODS ==================

    /**
     * Simple List to Page conversion - no fancy logic
     */
    private Page<ActivityLog> convertToPage(List<ActivityLog> allResults, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allResults.size());

        if (start > allResults.size()) {
            return Page.empty(pageable);
        }

        List<ActivityLog> pageContent = allResults.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allResults.size());
    }
}