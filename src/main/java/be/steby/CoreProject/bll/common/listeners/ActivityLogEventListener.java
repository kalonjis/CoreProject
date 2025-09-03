package be.steby.CoreProject.bll.common.listeners;

import be.steby.CoreProject.bll.common.events.UserActionEvent;
import be.steby.CoreProject.bll.common.helpers.ActivityLogDeviceHelper;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Global listener that handles ALL user action events asynchronously
 * This single listener processes events from all domains: AUTH, PASSWORD, EMAIL, etc.
 * Uses dedicated activityLogExecutor thread pool for optimal performance.
 *
 * Enhanced with device detection optimization to minimize database queries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogEventListener {

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogDeviceHelper deviceHelper;

    /**
     * Handle all user action events asynchronously using dedicated activity log executor
     * This method processes events from all activity log domains
     * Enhanced with optimal device detection when device is not provided in event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserActionEvent(UserActionEvent event) {
        try {
            if (event.device() != null) {
                // Device already provided - process directly
                processEventWithDevice(event, event.device());
            } else {
                // Device not provided - detect it optimally using cache and request context
                deviceHelper.executeWithDeviceDetection(
                        event.user(),
                        device -> processEventWithDevice(event, device)
                );
            }
        } catch (Exception e) {
            log.error("Failed to process UserActionEvent: {} for user {} - Error: {}",
                    event.actionType(),
                    event.user().getUsername(),
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    /**
     * Process the event with known device information (may be null)
     * @param event The user action event
     * @param device The device to use (may be null)
     */
    private void processEventWithDevice(UserActionEvent event, Device device) {
        try {
            // Create ActivityLog using the factory method from ActionLogType
            ActivityLog activityLog = event.actionType()
                    .createActivityLog(event.user(), event.successful());

            // Enrich with device information
            if (device != null) {
                activityLog.setDevice(device);
            }

            // Add details if provided
            if (event.details() != null && !event.details().trim().isEmpty()) {
                activityLog.setDetails(event.details().trim());
            }

            // Add failure reason for failed actions
            if (!event.successful() && event.failureReason() != null && !event.failureReason().trim().isEmpty()) {
                activityLog.setFailureReason(event.failureReason().trim());
            }

            // Save to database
            ActivityLog savedLog = activityLogRepository.save(activityLog);

            // Log success (debug level to avoid spam)
            log.debug("Activity logged successfully: {} for user {} - {} (ID: {}) - Device: {}",
                    event.actionType(),
                    event.user().getUsername(),
                    event.successful() ? "SUCCESS" : "FAILED",
                    savedLog.getId(),
                    device != null ? device.getId() : "unknown"
            );

            // Log security events for failed actions
            if (!event.successful()) {
                log.warn("Security event failed: {} for user {} from device {} - Reason: {}",
                        event.actionType(),
                        event.user().getUsername(),
                        device != null ? device.getId() : "unknown",
                        event.failureReason()
                );
            }

        } catch (Exception e) {
            log.error("Failed to save activity log for user {} action {}: {}",
                    event.user().getUsername(),
                    event.actionType(),
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }
}