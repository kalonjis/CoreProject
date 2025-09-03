package be.steby.CoreProject.bll.domains.device.utils;

import be.steby.CoreProject.bll.domains.device.services.DeviceCacheService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Component responsible for evaluating device security levels and trust requirements.
 * Uses caching to optimize repeated device lookups within the same session.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceSecurityEvaluator {

    private final DeviceCacheService deviceCacheService;

    /**
     * Checks if the device has the required trust level for the current request.
     * @param authentication The current authentication object
     * @param request The HTTP request containing JWT claims
     * @param requiredLevel The minimum trust level required
     * @return true if device meets the trust requirement, false otherwise
     */
    public boolean hasRequiredTrustLevel(Authentication authentication, HttpServletRequest request, DeviceTrustLevel requiredLevel) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Authentication is null or not authenticated");
            return false;
        }

        Claims claims = (Claims) request.getAttribute("jwt_claims");
        if (claims == null) {
            log.debug("No JWT claims found in request");
            return false;
        }

        Long deviceId = claims.get("deviceId", Long.class);
        if (deviceId == null) {
            log.debug("No device ID found in JWT claims");
            return false;
        }

        DeviceTrustLevel currentLevel = getDeviceTrustLevel(deviceId);
        if (currentLevel == null) {
            log.debug("Could not determine trust level for device ID: {}", deviceId);
            return false;
        }

        boolean hasRequiredLevel = currentLevel.ordinal() >= requiredLevel.ordinal();
        log.debug("Device {} trust level: {}, required: {}, has access: {}",
                deviceId, currentLevel, requiredLevel, hasRequiredLevel);

        return hasRequiredLevel;
    }

    /**
     * Gets the trust level for a specific device using cache optimization.
     * @param deviceId The device ID to check
     * @return The device trust level or null if device not found
     */
    public DeviceTrustLevel getDeviceTrustLevel(Long deviceId) {
        if (deviceId == null) {
            return null;
        }

        try {
            Device device = deviceCacheService.getDevice(deviceId);
            return device != null ? device.getDeviceTrustLevel() : null;
        } catch (Exception e) {
            log.warn("Failed to retrieve trust level for device {}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    /**
     * Gets a complete device object using cache optimization.
     * @param deviceId The device ID to retrieve
     * @return The device entity or null if not found
     */
    public Device getDevice(Long deviceId) {
        if (deviceId == null) {
            return null;
        }

        try {
            return deviceCacheService.getDevice(deviceId);
        } catch (Exception e) {
            log.warn("Failed to retrieve device {}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a device is confirmed for the current user.
     * @param authentication The current authentication
     * @param request The HTTP request
     * @return true if device is confirmed, false otherwise
     */
    public boolean isDeviceConfirmed(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Claims claims = (Claims) request.getAttribute("jwt_claims");
        if (claims == null) {
            return false;
        }

        // First check JWT claims for quick access
        Boolean confirmedInToken = claims.get("deviceConfirmed", Boolean.class);
        if (confirmedInToken != null) {
            return confirmedInToken;
        }

        // Fallback to database lookup
        Long deviceId = claims.get("deviceId", Long.class);
        if (deviceId == null) {
            return false;
        }

        Device device = getDevice(deviceId);
        return device != null && device.isConfirmed();
    }

    /**
     * Checks if a device is blacklisted.
     * @param deviceId The device ID to check
     * @return true if device is blacklisted, false otherwise
     */
    public boolean isDeviceBlacklisted(Long deviceId) {
        Device device = getDevice(deviceId);
        return device != null && device.isBlacklisted();
    }

    /**
     * Checks if a device is logged out.
     * @param deviceId The device ID to check
     * @return true if device is logged out, false otherwise
     */
    public boolean isDeviceLoggedOut(Long deviceId) {
        Device device = getDevice(deviceId);
        return device != null && device.isLoggedOut();
    }
}