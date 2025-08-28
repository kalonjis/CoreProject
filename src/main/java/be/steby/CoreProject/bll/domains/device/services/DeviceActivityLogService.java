package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.DeviceAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Device management domain activity logging service.
 * PURE logging service - records device events, nothing more.
 *
 * Principle: "We log device events, analysts analyze them"
 *
 * @author Steby Core Project Team
 * @version 2.0 - Pure KISS Edition
 */
@Service
@Slf4j
public class DeviceActivityLogService extends AbstractActivityLogService {

    // ================== CONSTRUCTOR ==================

    public DeviceActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "DEVICE";
    }

    // ================== DEVICE REGISTRATION LOGGING ==================

    /**
     * Logs new device registration
     */
    @Transactional
    public ActivityLog logDeviceRegistered(User user, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_REGISTERED, context);
        activityLog.setActionDetails("Device: " + device.getDeviceName() + " (" + device.getDeviceType() + ")");
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device verification request
     */
    @Transactional
    public ActivityLog logDeviceVerificationRequested(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_VERIFICATION_REQUESTED, context);
    }

    /**
     * Logs device verification completion
     */
    @Transactional
    public ActivityLog logDeviceVerificationCompleted(User user, Device device, boolean successful, String reason, RequestContext context) {
        if (successful) {
            return logSuccess(user, device, DeviceAction.DEVICE_VERIFICATION_COMPLETED, context);
        } else {
            return logFailure(user, device, DeviceAction.DEVICE_VERIFICATION_FAILED, reason, context);
        }
    }

    /**
     * Logs device verification expiration
     */
    @Transactional
    public ActivityLog logDeviceVerificationExpired(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_VERIFICATION_EXPIRED, context);
    }

    // ================== DEVICE MANAGEMENT LOGGING ==================

    /**
     * Logs device trusted status change
     */
    @Transactional
    public ActivityLog logDeviceTrusted(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_TRUSTED, context);
    }

    /**
     * Logs device untrusted status change
     */
    @Transactional
    public ActivityLog logDeviceUntrusted(User user, Device device, String reason, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_UNTRUSTED, context);
        activityLog.setActionDetails("Reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device suspension
     */
    @Transactional
    public ActivityLog logDeviceSuspended(User user, Device device, String reason, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_SUSPENDED, context);
        activityLog.setActionDetails("Suspension reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device reactivation
     */
    @Transactional
    public ActivityLog logDeviceReactivated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_REACTIVATED, context);
    }

    /**
     * Logs device removal
     */
    @Transactional
    public ActivityLog logDeviceRemoved(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_REMOVED, context);
    }

    /**
     * Logs device information update
     */
    @Transactional
    public ActivityLog logDeviceInfoUpdated(User user, Device device, String updatedFields, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_INFO_UPDATED, context);
        activityLog.setActionDetails("Updated: " + updatedFields);
        return activityLogRepository.save(activityLog);
    }

    // ================== DEVICE AUTHENTICATION LOGGING ==================

    /**
     * Logs device-based login
     */
    @Transactional
    public ActivityLog logDeviceLogin(User user, Device device, boolean successful, String reason, RequestContext context) {
        if (successful) {
            return logSuccess(user, device, DeviceAction.DEVICE_LOGIN_SUCCESS, context);
        } else {
            return logFailure(user, device, DeviceAction.DEVICE_LOGIN_FAILED, reason, context);
        }
    }

    /**
     * Logs device authentication challenge
     */
    @Transactional
    public ActivityLog logDeviceAuthChallenge(User user, Device device, String challengeType, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_AUTH_CHALLENGE, context);
        activityLog.setActionDetails("Challenge type: " + challengeType);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device authentication success
     */
    @Transactional
    public ActivityLog logDeviceAuthSuccess(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_AUTH_SUCCESS, context);
    }

    /**
     * Logs device authentication failure
     */
    @Transactional
    public ActivityLog logDeviceAuthFailed(User user, Device device, String reason, RequestContext context) {
        return logFailure(user, device, DeviceAction.DEVICE_AUTH_FAILED, reason, context);
    }

    // ================== DEVICE SECURITY LOGGING ==================

    /**
     * Logs suspicious device detection
     */
    @Transactional
    public ActivityLog logSuspiciousDeviceDetected(User user, Device device, String suspiciousActivity, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, DeviceAction.SUSPICIOUS_DEVICE_DETECTED, "Suspicious activity detected", context);
        activityLog.setActionDetails("Activity: " + suspiciousActivity);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device fingerprint change
     */
    @Transactional
    public ActivityLog logDeviceFingerprintChanged(User user, Device device, String reason, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_FINGERPRINT_CHANGED, context);
        activityLog.setActionDetails("Change reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device security scan
     */
    @Transactional
    public ActivityLog logDeviceSecurityScan(User user, Device device, boolean passed, String findings, RequestContext context) {
        if (passed) {
            ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_SECURITY_SCAN_PASSED, context);
            if (findings != null) {
                activityLog.setActionDetails("Scan results: " + findings);
                return activityLogRepository.save(activityLog);
            }
            return activityLog;
        } else {
            return logFailure(user, device, DeviceAction.DEVICE_SECURITY_SCAN_FAILED, findings, context);
        }
    }

    /**
     * Logs device compromise detection
     */
    @Transactional
    public ActivityLog logDeviceCompromiseDetected(User user, Device device, String evidence, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, DeviceAction.DEVICE_COMPROMISE_DETECTED, "Device compromise detected", context);
        activityLog.setActionDetails("Evidence: " + evidence);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device quarantine
     */
    @Transactional
    public ActivityLog logDeviceQuarantined(User user, Device device, String reason, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_QUARANTINED, context);
        activityLog.setActionDetails("Quarantine reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device quarantine release
     */
    @Transactional
    public ActivityLog logDeviceQuarantineReleased(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_QUARANTINE_RELEASED, context);
    }

    // ================== DEVICE LOCATION LOGGING ==================

    /**
     * Logs device location update
     */
    @Transactional
    public ActivityLog logDeviceLocationUpdated(User user, Device device, String location, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_LOCATION_UPDATED, context);
        activityLog.setActionDetails("New location: " + location);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs unusual device location
     */
    @Transactional
    public ActivityLog logUnusualDeviceLocation(User user, Device device, String location, String reason, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.UNUSUAL_DEVICE_LOCATION, context);
        activityLog.setActionDetails("Location: " + location + " - Reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    // ================== DEVICE SESSION LOGGING ==================

    /**
     * Logs device session start
     */
    @Transactional
    public ActivityLog logDeviceSessionStarted(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_SESSION_STARTED, context);
    }

    /**
     * Logs device session end
     */
    @Transactional
    public ActivityLog logDeviceSessionEnded(User user, Device device, String reason, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, DeviceAction.DEVICE_SESSION_ENDED, context);
        activityLog.setActionDetails("End reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device session timeout
     */
    @Transactional
    public ActivityLog logDeviceSessionTimeout(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_SESSION_TIMEOUT, context);
    }

    /**
     * Logs concurrent device limit exceeded
     */
    @Transactional
    public ActivityLog logDeviceLimitExceeded(User user, Device device, int currentCount, int maxAllowed, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, DeviceAction.DEVICE_LIMIT_EXCEEDED, "Too many concurrent devices", context);
        activityLog.setActionDetails("Current: " + currentCount + " Max: " + maxAllowed);
        return activityLogRepository.save(activityLog);
    }

    // ================== DEVICE ADMIN ACTIONS LOGGING ==================

    /**
     * Logs device force removal by admin
     */
    @Transactional
    public ActivityLog logDeviceForceRemoved(User admin, User targetUser, Device device, String reason, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, DeviceAction.DEVICE_FORCE_REMOVED, context);
        activityLog.setActionDetails("Removed device for user: " + targetUser.getUsername() + " - Reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs device admin override
     */
    @Transactional
    public ActivityLog logDeviceAdminOverride(User admin, User targetUser, Device device, String action, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, DeviceAction.DEVICE_ADMIN_OVERRIDE, context);
        activityLog.setActionDetails("Admin action: " + action + " for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }
}