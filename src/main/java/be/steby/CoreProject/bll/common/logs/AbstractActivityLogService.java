package be.steby.CoreProject.bll.common.logs;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.utils.DeviceDetectionHelper;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.ActionLogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Abstract base class providing core activity logging functionality.
 * KISS principle: Simple, focused, includes DRY methods for device detection.
 *
 * ✅ PRODUCTION READY: Logs work regardless of debug level
 *
 * @author Steby Core Project Team
 * @version 3.0 - PRODUCTION Edition
 */
@Slf4j
public abstract class AbstractActivityLogService {

    // ================== DEPENDENCIES ==================

    protected final ActivityLogRepository activityLogRepository;
    protected final DeviceDetectionHelper deviceDetectionHelper;

    // ================== CONSTRUCTOR ==================

    protected AbstractActivityLogService(ActivityLogRepository activityLogRepository,
                                         DeviceDetectionHelper deviceDetectionHelper) {
        this.activityLogRepository = activityLogRepository;
        this.deviceDetectionHelper = deviceDetectionHelper;
    }

    // ================== ABSTRACT METHODS ==================

    /**
     * Returns the domain name for this service (e.g., "AUTH", "ADMIN", "ACCOUNT")
     */
    protected abstract String getDomainName();

    // ================== CORE LOGGING METHODS ==================

    /**
     * Log successful action
     */
    @Transactional
    protected ActivityLog logSuccess(User user, Device device, ActionLogType actionType, RequestContext context) {
        return logUserAction(user, device, actionType, true, null, null, context);
    }

    /**
     * Log failed action
     */
    @Transactional
    protected ActivityLog logFailure(User user, Device device, ActionLogType actionType, String reason, RequestContext context) {
        return logUserAction(user, device, actionType, false, reason, null, context);
    }

    /**
     * Main method for logging user actions - SIMPLE and focused
     */
    @Transactional
    public ActivityLog logUserAction(User user, Device device, ActionLogType actionType,
                                     boolean successful, String details,
                                     String additionalData, RequestContext context) {

        log.debug("Logging {} action for user {}: {}",
                getDomainName(), user.getUsername(), ((Enum<?>) actionType).name());

        // Create activity log using ActionLogType factory method
        ActivityLog activityLog = actionType.createActivityLogWithContext(user, successful, context);

        // Simple enrichment
        if (device != null) {
            activityLog.setDevice(device);
        }

        if (details != null) {
            activityLog.setActionDetails(details);
        }

        if (additionalData != null) {
            activityLog.setMetadata(additionalData);
        }

        return activityLogRepository.save(activityLog);
    }

    // ================== DRY METHODS FOR LISTENERS - BUG CORRIGÉ ==================

    /**
     * ✅ DRY method: Log with device detection when device might be in event
     * ✅ PRODUCTION SAFE: Works regardless of log level
     */
    public void logWithDeviceDetection(User user,
                                       Device providedDevice,
                                       RequestContext requestContext,
                                       BiConsumer<User, Device> logAction,
                                       String actionDescription) {
        if (providedDevice != null) {
            // ✅ Device provided by event - direct use
            logAction.accept(user, providedDevice);
        } else {
            // ✅ Device detection needed
            deviceDetectionHelper.executeWithDeviceDetection(
                    user,
                    requestContext,
                    device -> {
                        // ✅ Debug log CONDITIONNEL (pas critique)
                        if (log.isDebugEnabled()) {
                            log.debug("Logging {} for user: {} from device: {}",
                                    actionDescription,
                                    user.getEmail(),
                                    device != null ? device.getOsVersion() + device : "unknown");
                        }

                        // ✅ ACTION PRINCIPALE - TOUJOURS EXÉCUTÉE !
                        logAction.accept(user, device);
                    });
        }
    }

    /**
     * ✅ DRY method: Log with device detection when no device in event
     * ✅ PRODUCTION SAFE: Works regardless of log level
     */
    public void logWithDeviceDetection(User user,
                                       RequestContext requestContext,
                                       Consumer<Device> logAction,
                                       String actionDescription) {
        deviceDetectionHelper.executeWithDeviceDetection(
                user,
                requestContext,
                device -> {
                    // ✅ Debug log CONDITIONNEL (pas critique)
                    if (log.isDebugEnabled()) {
                        log.debug("Logging {} for user: {} from device: {}",
                                actionDescription,
                                user.getEmail(),
                                device != null ? device.getDeviceName() : "unknown");
                    }

                    // ✅ ACTION PRINCIPALE - TOUJOURS EXÉCUTÉE !
                    logAction.accept(device);
                });
    }
}