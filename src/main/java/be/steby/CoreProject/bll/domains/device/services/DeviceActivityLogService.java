package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.device.events.*;
import be.steby.CoreProject.bll.domains.device.listeners.DeviceActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.DeviceAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Device domain activity log service.
 *
 * <p>Translates device domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps one-to-one to a {@link DeviceAction} constant.</p>
 *
 * <p>Called exclusively from {@link DeviceActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 */
@Service
@Slf4j
public class DeviceActivityLogService extends ActivityLogService {

    public DeviceActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "DEVICE";
    }

    // =========================================================================
    // Confirmation
    // =========================================================================

    /**
     * Persists a {@link DeviceAction#DEVICE_CONFIRMED} entry when the user
     * confirms a device via the email confirmation link.
     *
     * <p>{@code actorDevice} is the device from which the link was clicked;
     * it may differ from the confirmed device.</p>
     *
     * @param event contains the user, actor device and confirmed device; all never null
     */
    public void logDeviceConfirmed(DeviceConfirmedEvent event) {
        logUserActivity(event.user(), event.actorDevice(), DeviceAction.DEVICE_CONFIRMED,
                true, null, event.device());
        log.debug("DEVICE_CONFIRMED logged — user: {}, actorDevice: {}, targetDevice: {}",
                event.user().getUsername(), event.actorDevice().getId(), event.device().getId());
    }

    /**
     * Persists a {@link DeviceAction#DEVICE_REJECTED} entry when the user
     * rejects a device via the email confirmation link.
     *
     * <p>{@code actorDevice} is the device from which the link was clicked;
     * it may differ from the rejected device. At this point the target device
     * is already blacklisted and its tokens revoked.</p>
     *
     * @param event contains the user, actor device and rejected device; all never null
     */
    public void logDeviceRejected(DeviceRejectedEvent event) {
        logUserActivity(event.user(), event.actorDevice(), DeviceAction.DEVICE_REJECTED,
                true, null, event.device());
        log.debug("DEVICE_REJECTED logged — user: {}, actorDevice: {}, targetDevice: {}",
                event.user().getUsername(), event.actorDevice().getId(), event.device().getId());
    }

    // =========================================================================
    // Trust level
    // =========================================================================

    /**
     * Persists a {@link DeviceAction#DEVICE_TRUST_LEVEL_UPDATED} entry when
     * the user changes the trust level of one of their devices.
     *
     * <p>The transition ({@code oldLevel → newLevel}) is stored in
     * {@code actionDetails} for audit traceability.</p>
     *
     * @param event contains the user, actor device, target device and both trust levels; never null
     */
    public void logTrustLevelUpdated(DeviceTrustLevelChangedEvent event) {
        String details = event.oldLevel() + " → " + event.newTrustLevel().name();
        logUserActivity(event.user(), event.actorDevice(), DeviceAction.DEVICE_TRUST_LEVEL_UPDATED,
                true, details, event.targetDevice());
        log.debug("DEVICE_TRUST_LEVEL_UPDATED logged — user: {}, actorDevice: {}, targetDevice: {}, transition: {}",
                event.user().getUsername(), event.actorDevice().getId(), event.targetDevice().getId(), details);
    }

    // =========================================================================
    // Disconnection
    // =========================================================================

    /**
     * Persists a {@link DeviceAction#DEVICE_DISCONNECTED} entry when the user
     * remotely disconnects a specific device.
     *
     * @param event contains the authenticated user, actor device and disconnected device; all never null
     */
    public void logDeviceDisconnected(DeviceDisconnectedEvent event) {
        logUserActivity(event.user(), event.actorDevice(), DeviceAction.DEVICE_DISCONNECTED,
                true, null, event.disconnectedDevice());
        log.debug("DEVICE_DISCONNECTED logged — user: {}, actorDevice: {}, targetDevice: {}",
                event.user().getUsername(), event.actorDevice().getId(), event.disconnectedDevice().getId());
    }

    /**
     * Persists a {@link DeviceAction#DEVICE_ALL_OTHERS_DISCONNECTED} entry when
     * the user disconnects all devices except their current one.
     *
     * <p>No {@code targetDevice} is set since multiple devices are affected.
     * The number of disconnected devices is stored in {@code actionDetails}.</p>
     *
     * @param event contains the user, current device and disconnected count; never null
     */
    public void logAllOthersDisconnected(DeviceAllOthersDisconnectedEvent event) {
        String details = "disconnected: " + event.disconnectedCount();
        logUserActivity(event.user(), event.currentDevice(), DeviceAction.DEVICE_ALL_OTHERS_DISCONNECTED,
                true, details);
        log.debug("DEVICE_ALL_OTHERS_DISCONNECTED logged — user: {}, count: {}",
                event.user().getUsername(), event.disconnectedCount());
    }

    // =========================================================================
    // Confirmation link
    // =========================================================================

    /**
     * Persists a {@link DeviceAction#DEVICE_CONFIRMATION_LINK_REQUESTED} entry
     * when the user manually requests a new device confirmation link.
     *
     * <p>Actor and target are the same device — the user is requesting a new
     * link for their own current device.</p>
     *
     * @param event contains the user and the device awaiting confirmation; both never null
     */
    public void logConfirmationLinkRequested(DeviceConfirmationLinkRequestedEvent event) {
        logUserActivity(event.user(), event.device(), DeviceAction.DEVICE_CONFIRMATION_LINK_REQUESTED,
                true, null, null);
        log.debug("DEVICE_CONFIRMATION_LINK_REQUESTED logged — user: {}, device: {}",
                event.user().getUsername(), event.device().getId());
    }
}