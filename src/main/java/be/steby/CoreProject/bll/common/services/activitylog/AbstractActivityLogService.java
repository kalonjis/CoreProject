package be.steby.CoreProject.bll.common.services.activitylog;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.utils.IpLocationUtils;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Abstract class providing basic functionality for user activity logging.
 * This class is designed to be extended by domain-specific services.
 *
 * Updated to include device detection helper functionality directly.
 */
@Slf4j
public abstract class AbstractActivityLogService {

    protected final ActivityLogRepository activityLogRepository;
    protected final ObjectMapper objectMapper;

    // Optional DeviceService for device detection in async contexts
    // Subclasses that need device detection should inject this service
    protected DeviceService deviceService;

    /**
     * Constructor for basic initialization of common dependencies.
     *
     * @param activityLogRepository Repository for activity log persistence
     */
    protected AbstractActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor with DeviceService for subclasses that need device detection
     *
     * @param activityLogRepository Repository for activity log persistence
     * @param deviceService Service for device detection (optional)
     */
    protected AbstractActivityLogService(ActivityLogRepository activityLogRepository, DeviceService deviceService) {
        this.activityLogRepository = activityLogRepository;
        this.objectMapper = new ObjectMapper();
        this.deviceService = deviceService;
    }

    /**
     * Main method for logging user actions.
     * This method is used by all specific logging methods.
     *
     * @param user User who performed the action
     * @param device Device used (can be null)
     * @param actionType Type of action
     * @param successful Whether the action succeeded or failed
     * @param details Additional details about the action
     * @param metadata Metadata in JSON format (can be null)
     * @param requestContext Request context
     * @return The created log entry
     */
    @Transactional
    public ActivityLog logUserAction(
            User user,
            Device device,
            ActionLogType actionType,
            boolean successful,
            String details,
            String metadata,
            RequestContext requestContext) {

        String clientIp = getClientIpAddress(requestContext);
        String location = IpLocationUtils.resolveLocationFromIp(clientIp);
        String sessionId = getOrCreateSessionId(requestContext);

        // Evaluate risk level based on action type and context
        int riskLevel = evaluateRiskLevel(user, device, actionType, requestContext);
        boolean triggeredAlert = riskLevel >= 3; // Alert if high risk level

        if (triggeredAlert) {
            log.warn("High-risk action detected: {} for user {}, risk level {}",
                    actionType, user.getUsername(), riskLevel);
        }

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .device(device)
                .timestamp(Instant.now())
                .ipAddress(clientIp)
                .location(location)
                .successful(successful)
                .failureReason(successful ? null : details)
                .actionDetails(details)
                .metadata(metadata)
                .actionType(actionType)
                .sessionId(sessionId)
                .riskLevel(riskLevel)
                .triggeredAlert(triggeredAlert)
                .build();

        activityLogRepository.save(activityLog);

        if (triggeredAlert) {
            try {
                handleHighRiskActivity(activityLog, user, device, requestContext);
            } catch (Exception e) {
                log.error("Error handling high-risk activity alert: {}", e.getMessage(), e);
            }
        }

        return activityLog;
    }

    /**
     * Helper method for executing actions with device detection.
     * Replaces the separate DeviceDetectionHelper class.
     *
     * If device detection fails, the action is still executed with a null device.
     *
     * @param user User concerned
     * @param requestContext Request context for device detection
     * @param action Action to execute with the device (can be null)
     */
    protected void executeWithDeviceDetection(User user,
                                              RequestContext requestContext,
                                              Consumer<Device> action) {
        if (deviceService == null) {
            log.warn("DeviceService not available for device detection. Executing action with null device.");
            action.accept(null);
            return;
        }

        Device device = null;

        try {
            device = deviceService.detectFromRequestContext(requestContext, user);
        } catch (Exception e) {
            log.warn("Unable to detect device for user {} : {}",
                    user.getUsername(), e.getMessage());
            // Continue with null device
        }

        // Execute action with device (which can be null)
        action.accept(device);
    }

    /**
     * Overloaded method that combines device detection with logging
     *
     * @param user User concerned
     * @param requestContext Request context
     * @param actionType Type of action to log
     * @param successful Whether action was successful
     * @param details Action details
     * @param metadata Optional metadata
     * @return The created ActivityLog
     */
    protected ActivityLog logWithDeviceDetection(User user,
                                                 RequestContext requestContext,
                                                 ActionLogType actionType,
                                                 boolean successful,
                                                 String details,
                                                 String metadata) {
        if (deviceService == null) {
            return logUserAction(user, null, actionType, successful, details, metadata, requestContext);
        }

        try {
            Device device = deviceService.detectFromRequestContext(requestContext, user);
            return logUserAction(user, device, actionType, successful, details, metadata, requestContext);
        } catch (Exception e) {
            log.warn("Unable to detect device for user {} during logging: {}",
                    user.getUsername(), e.getMessage());
            return logUserAction(user, null, actionType, successful, details, metadata, requestContext);
        }
    }

    // =========================================================================
    // Abstract and protected methods for subclasses
    // =========================================================================

    /**
     * Returns the domain name for this service.
     * Must be implemented by subclasses.
     *
     * @return Domain name
     */
    protected abstract String getDomainName();

    /**
     * Handles high-risk activities. Can be overridden by subclasses.
     *
     * @param activityLog The log entry that triggered the alert
     * @param user User involved
     * @param device Device involved (can be null)
     * @param requestContext Request context
     */
    protected void handleHighRiskActivity(ActivityLog activityLog, User user, Device device, RequestContext requestContext) {
        // Default implementation - log warning
        log.warn("High-risk activity logged for user {}: {} from IP {}",
                user.getUsername(), activityLog.getActionType(), activityLog.getIpAddress());

        // Subclasses can override this to implement specific alert mechanisms
        // (email notifications, admin alerts, etc.)
    }

    /**
     * Evaluates risk level based on various factors
     */
    protected int evaluateRiskLevel(User user, Device device, ActionLogType actionType, RequestContext requestContext) {
        int riskLevel = 0;

        // Base risk by action type
        riskLevel += getBaseRiskForActionType(actionType);

        // Device-related risk
        if (device == null) {
            riskLevel += 1; // Unknown device
        } else if (!device.isConfirmed()) {
            riskLevel += 2; // Unconfirmed device
        } else if (device.isBlacklisted()) {
            riskLevel += 3; // Blacklisted device
        }

        // IP-based risk (could be enhanced with IP reputation services)
        String clientIp = getClientIpAddress(requestContext);
        if (!IpLocationUtils.isPrivateOrLocalIp(clientIp)) {
            // Check for rapid IP changes, unusual locations, etc.
            riskLevel += evaluateIpRisk(user, clientIp);
        }

        return Math.min(riskLevel, 5); // Cap at 5
    }

    /**
     * Returns base risk level for action type
     */
    protected abstract int getBaseRiskForActionType(ActionLogType actionType);

    /**
     * Evaluates IP-related risk factors
     */
    protected int evaluateIpRisk(User user, String clientIp) {
        return 0;
    }

    // =========================================================================
    // Utility methods
    // =========================================================================

    protected String getClientIpAddress(RequestContext requestContext) {
        return requestContext != null ? requestContext.getClientIp() : "Unknown";
    }

    protected String getOrCreateSessionId(RequestContext requestContext) {
        if (requestContext != null && requestContext.getSessionId() != null) {
            return requestContext.getSessionId();
        }
        return "session_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    // =========================================================================
    // Query methods
    // =========================================================================

    /**
     * Gets user activity logs with pagination
     */
    public Page<ActivityLog> getUserActivityLogs(User user, Pageable pageable) {
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

//    /**
//     * Gets user activity logs for specific actions
//     */
//    public List<ActivityLog> getUserActivityLogs(User user, List<ActionLogType> actionTypes, Instant since) {
//        return activityLogRepository.findByUserAndActionTypeInAndTimestampAfterOrderByTimestampDesc(
//                user, actionTypes, since);
//    }
//
//    /**
//     * Gets suspicious activities (high risk level)
//     */
//    public List<ActivityLog> getSuspiciousActivities(Instant since, int minRiskLevel) {
//        return activityLogRepository.findByRiskLevelGreaterThanEqualAndTimestampAfterOrderByTimestampDesc(
//                minRiskLevel, since);
//    }
//
//    // =========================================================================
//    // Cleanup methods
//    // =========================================================================
//
//    /**
//     * Cleans up old activity logs based on retention policy
//     */
//    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
//    @Transactional
//    public void cleanupOldLogs() {
//        try {
//            LocalDate cutoffDate = LocalDate.now().minus(getLogRetentionDays(), ChronoUnit.DAYS);
//            Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
//
//            int deletedCount = activityLogRepository.deleteByTimestampBefore(cutoffInstant);
//            log.info("Cleaned up {} old activity logs older than {}", deletedCount, cutoffDate);
//        } catch (Exception e) {
//            log.error("Error during activity log cleanup: {}", e.getMessage(), e);
//        }
//    }

    /**
     * Returns the number of days to retain logs. Can be overridden by subclasses.
     */
    protected long getLogRetentionDays() {
        return 365; // Default: 1 year
    }
}