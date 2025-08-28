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
 * Device domain activity logging service - KISS VERSION
 * Uses ONLY DeviceAction enum values
 */
@Service
@Slf4j
public class DeviceActivityLogService extends AbstractActivityLogService {

    public DeviceActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "DEVICE";
    }

    // ================== DEVICE DOMAIN METHODS ONLY ==================

    @Transactional
    public ActivityLog logDeviceRegistered(User user, Device device, RequestContext context) {
        ActivityLog log = logSuccess(user, device, DeviceAction.DEVICE_REGISTERED, context);
        log.setActionDetails("Device: " + device.getDeviceBrand() + " (" + device.getDeviceType() + ")");
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logDeviceTrusted(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.DEVICE_TRUSTED, context);
    }

    @Transactional
    public ActivityLog logDeviceBlocked(User user, Device device, String reason, RequestContext context) {
        ActivityLog log = logFailure(user, device, DeviceAction.DEVICE_BLOCKED, reason, context);
        return log;
    }

    @Transactional
    public ActivityLog logNewDeviceLogin(User user, Device device, RequestContext context) {
        return logSuccess(user, device, DeviceAction.NEW_DEVICE_LOGIN, context);
    }

    @Transactional
    public ActivityLog logSuspiciousDeviceDetected(User user, Device device, String reason, RequestContext context) {
        return logFailure(user, device, DeviceAction.SUSPICIOUS_DEVICE_DETECTED, reason, context);
    }
}