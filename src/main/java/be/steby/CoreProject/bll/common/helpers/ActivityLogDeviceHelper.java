package be.steby.CoreProject.bll.common.helpers;

import be.steby.CoreProject.bll.domains.device.services.DeviceCacheService;
import be.steby.CoreProject.bll.domains.device.utils.DeviceRequestUtils;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Consumer;

/**
 * Helper component for activity logging that optimally retrieves device information.
 * Provides methods to execute logging actions with device context, using caching
 * and request attributes to minimize database queries.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityLogDeviceHelper {

    private final DeviceCacheService deviceCacheService;

    /**
     * Executes a logging action with device detection optimized for performance.
     * First tries to get device from request attributes (set by JwtFilter),
     * then falls back to cache/database lookup if needed.
     *
     * @param user The user for the activity log
     * @param loggingAction The action to execute with the device
     */
    public void executeWithDeviceDetection(User user, Consumer<Device> loggingAction) {
        Device device = null;

        try {
            // Try to get current HTTP request
            HttpServletRequest request = getCurrentHttpRequest();

            if (request != null) {
                // First attempt: Get device from request attributes (fastest)
                device = DeviceRequestUtils.getCurrentDevice(request);

                if (device == null) {
                    // Second attempt: Get device ID from JWT claims and use cache
                    Long deviceId = DeviceRequestUtils.getDeviceIdFromClaims(request);
                    if (deviceId != null) {
                        device = deviceCacheService.getDevice(deviceId);

                        // Store in request for subsequent uses within same request
                        if (device != null) {
                            DeviceRequestUtils.setCurrentDevice(request, device);
                        }
                    }
                }
            }

            if (device == null) {
                log.debug("No device found for user {} activity logging", user.getUsername());
            } else {
                log.debug("Found device {} for user {} activity logging", device.getId(), user.getUsername());
            }

        } catch (Exception e) {
            log.warn("Failed to detect device for user {} activity logging: {}",
                    user.getUsername(), e.getMessage());
            // Continue with null device
        }

        // Execute the logging action with the device (which may be null)
        loggingAction.accept(device);
    }

    /**
     * Executes a logging action with a specific device ID.
     * Uses cache optimization to retrieve the device.
     *
     * @param user The user for the activity log
     * @param deviceId The specific device ID to use
     * @param loggingAction The action to execute with the device
     */
    public void executeWithSpecificDevice(User user, Long deviceId, Consumer<Device> loggingAction) {
        Device device = null;

        try {
            if (deviceId != null) {
                device = deviceCacheService.getDevice(deviceId);

                if (device == null) {
                    log.warn("Device with ID {} not found for user {} activity logging",
                            deviceId, user.getUsername());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve device {} for user {} activity logging: {}",
                    deviceId, user.getUsername(), e.getMessage());
        }

        // Execute the logging action with the device (which may be null)
        loggingAction.accept(device);
    }

    /**
     * Executes a logging action when the device is already available.
     * Simply validates the device and executes the action.
     *
     * @param user The user for the activity log
     * @param device The device to use (may be null)
     * @param loggingAction The action to execute with the device
     */
    public void executeWithKnownDevice(User user, Device device, Consumer<Device> loggingAction) {
        if (device != null) {
            log.debug("Using known device {} for user {} activity logging", device.getId(), user.getUsername());
        } else {
            log.debug("No device provided for user {} activity logging", user.getUsername());
        }

        // Execute the logging action with the device
        loggingAction.accept(device);
    }

    /**
     * Gets the current HTTP request from Spring's RequestContextHolder.
     * Returns null if no request context is available (e.g., in background threads).
     *
     * @return The current HTTP request or null
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("No HTTP request context available: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if device information is available in the current request context.
     *
     * @return true if device information is available, false otherwise
     */
    public boolean isDeviceAvailableInContext() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            return false;
        }

        return DeviceRequestUtils.hasCurrentDevice(request) ||
                DeviceRequestUtils.getDeviceIdFromClaims(request) != null;
    }

    /**
     * Gets the current device from request context if available.
     *
     * @return The current device or null
     */
    public Device getCurrentDeviceFromContext() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            return null;
        }

        Device device = DeviceRequestUtils.getCurrentDevice(request);
        if (device != null) {
            return device;
        }

        // Try to get from claims and cache
        Long deviceId = DeviceRequestUtils.getDeviceIdFromClaims(request);
        if (deviceId != null) {
            return deviceCacheService.getDevice(deviceId);
        }

        return null;
    }
}