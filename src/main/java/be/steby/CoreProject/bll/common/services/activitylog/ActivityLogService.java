package be.steby.CoreProject.bll.common.services.activitylog;

import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.ActionLogType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base service for persisting activity log entries across all domains.
 *
 * <p>All writes are asynchronous and transactional. Domain-specific services
 * extend this class and call {@link #logUserActivity} with the appropriate
 * parameters for their context.</p>
 *
 * <p>{@code user} may be null for system-level events (e.g. blocked IPs).
 * {@code device} and {@code targetDevice} may be null for token-based flows
 * or events that do not involve a specific device.</p>
 */
@RequiredArgsConstructor
@Slf4j
public abstract class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Persists an activity log entry for a single-device action.
     *
     * @param user          the user performing the action; null for system events
     * @param device        the device from which the action was initiated; null if not applicable
     * @param actionLogType the type of action
     * @param successful    whether the action completed successfully
     * @param details       failure reason when {@code successful} is false;
     *                      contextual details when {@code successful} is true; null if none
     */
    @Transactional
    public void logUserActivity(User user, Device device, ActionLogType actionLogType,
                                boolean successful) {
        buildAndSave(user, device, null, actionLogType, successful, null);
    }

    @Transactional
    public void logUserActivity(User user, Device device, ActionLogType actionLogType,
                                boolean successful, String details) {
        buildAndSave(user, device, null, actionLogType, successful, details);
    }

    /**
     * Persists an activity log entry for a two-device action.
     *
     * <p>Use this overload when the action involves two distinct devices — for example,
     * remotely disconnecting a device or changing another device's trust level.
     * {@code actorDevice} is the session origin; {@code targetDevice} is the device
     * affected by the action.</p>
     *
     * @param user          the user performing the action; null for system events
     * @param actorDevice   the device from which the action was initiated; null if not applicable
     * @param actionLogType the type of action
     * @param successful    whether the action completed successfully
     * @param details       failure reason when {@code successful} is false;
     *                      contextual details when {@code successful} is true; null if none
     * @param targetDevice  the device targeted by the action
     */
    @Transactional
    public void logUserActivity(User user, Device actorDevice, ActionLogType actionLogType,
                                boolean successful, String details, Device targetDevice) {
        buildAndSave(user, actorDevice, targetDevice, actionLogType, successful, details);
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private void buildAndSave(User user, Device actorDevice, Device targetDevice,
                              ActionLogType actionLogType, boolean successful, String details) {
        try {
            ActivityLog activityLog = ActivityLog.builderWithTimestamp()
                    .user(user)
                    .device(actorDevice)
                    .targetDevice(targetDevice)
                    .actionType(actionLogType.getName())
                    .actionCategory(actionLogType.getCategory())
                    .successful(successful)
                    .failureReason(successful ? null : details)
                    .actionDetails(successful ? details : null)
                    .ipAddress(actorDevice != null ? actorDevice.getLastIpAddress() : null)
                    .location(actorDevice != null ? actorDevice.getLocation() : null)
                    .build();

            activityLogRepository.save(activityLog);

            log.debug("Activity logged: user={}, action={}, successful={}",
                    user != null ? user.getUsername() : "system",
                    actionLogType.getName(), successful);

        } catch (Exception e) {
            log.error("Failed to log activity: user={}, action={}",
                    user != null ? user.getUsername() : "system",
                    actionLogType.getName(), e);
        }
    }

    /**
     * Returns the domain name used for logging context.
     * Implemented by each domain-specific subclass.
     */
    protected abstract String getDomainName();
}