package be.steby.CoreProject.bll.domains.device.listeners;

import be.steby.CoreProject.bll.domains.device.events.*;
import be.steby.CoreProject.bll.domains.device.services.DeviceActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for device domain activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link DeviceActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after
 * {@link DeviceNotificationListener} ({@code @Order(10)}) — a logging failure
 * never blocks email delivery.</p>
 *
 * Covered events → DeviceAction mapping:
 * <ul>
 *   <li>{@link DeviceConfirmedEvent}                  → DEVICE_CONFIRMED</li>
 *   <li>{@link DeviceRejectedEvent}                   → DEVICE_REJECTED</li>
 *   <li>{@link DeviceTrustLevelChangedEvent}          → DEVICE_TRUST_LEVEL_UPDATED</li>
 *   <li>{@link DeviceDisconnectedEvent}               → DEVICE_DISCONNECTED</li>
 *   <li>{@link DeviceAllOthersDisconnectedEvent}      → DEVICE_ALL_OTHERS_DISCONNECTED</li>
 *   <li>{@link DeviceConfirmationLinkRequestedEvent}  → DEVICE_CONFIRMATION_LINK_REQUESTED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DeviceActivityLogListener {

    private final DeviceActivityLogService deviceActivityLogService;

    // =========================================================================
    // Confirmation
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleDeviceConfirmed(DeviceConfirmedEvent event) {
        try {
            log.debug("Processing device confirmed event for user: {}", event.user().getUsername());
            deviceActivityLogService.logDeviceConfirmed(event);
        } catch (Exception e) {
            log.error("Failed to log DEVICE_CONFIRMED for user: {}", event.user().getUsername(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleDeviceRejected(DeviceRejectedEvent event) {
        try {
            log.debug("Processing device rejected event for user: {}", event.user().getUsername());
            deviceActivityLogService.logDeviceRejected(event);
        } catch (Exception e) {
            log.error("Failed to log DEVICE_REJECTED for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Trust level
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleTrustLevelChanged(DeviceTrustLevelChangedEvent event) {
        try {
            log.debug("Processing trust level changed event for user: {}", event.user().getUsername());
            deviceActivityLogService.logTrustLevelUpdated(event);
        } catch (Exception e) {
            log.error("Failed to log DEVICE_TRUST_LEVEL_UPDATED for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Disconnection
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleDeviceDisconnected(DeviceDisconnectedEvent event) {
        try {
            log.debug("Processing device disconnected event for user: {}", event.user().getUsername());
            deviceActivityLogService.logDeviceDisconnected(event);
        } catch (Exception e) {
            log.error("Failed to log DEVICE_DISCONNECTED for user: {}", event.user().getUsername(), e);
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleAllOthersDisconnected(DeviceAllOthersDisconnectedEvent event) {
        try {
            log.debug("Processing all-others-disconnected event for user: {}", event.user().getUsername());
            deviceActivityLogService.logAllOthersDisconnected(event);
        } catch (Exception e) {
            log.error("Failed to log DEVICE_ALL_OTHERS_DISCONNECTED for user: {}", event.user().getUsername(), e);
        }
    }

    // =========================================================================
    // Confirmation link
    // =========================================================================

    @EventListener
    @Async("activityLogExecutor")
    public void handleConfirmationLinkRequested(DeviceConfirmationLinkRequestedEvent event) {
        try {
            log.debug("Processing confirmation link requested event for user: {}", event.user().getUsername());
            deviceActivityLogService.logConfirmationLinkRequested(event);
        } catch (Exception e) {
            log.error("Failed to log DEVICE_CONFIRMATION_LINK_REQUESTED for user: {}", event.user().getUsername(), e);
        }
    }
}