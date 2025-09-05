package be.steby.CoreProject.bll.domains.device.utils;

import be.steby.CoreProject.bll.domains.device.models.DeviceSecurityResult;
import be.steby.CoreProject.bll.domains.device.services.DeviceSecurityService;
import be.steby.CoreProject.bll.domains.device.utils.DeviceContextProvider;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Security evaluator for device-based authorization decisions.
 * This component provides utilities for checking device trust levels and security status
 * in a centralized and secure manner using the shared device cache.
 *
 * Refactored to use the centralized DeviceSecurityService instead of maintaining
 * its own cache, ensuring consistency and reducing redundancy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceSecurityEvaluator {

    private final DeviceSecurityService deviceSecurityService;

    /**
     * Checks if the current device meets the required trust level.
     * This method is typically used in security annotations and authorization logic.
     *
     * @param authentication Current authentication context
     * @param request HTTP request containing device information
     * @param requiredLevel Minimum trust level required
     * @return true if device meets or exceeds the required trust level, false otherwise
     */
    public boolean hasRequiredTrustLevel(Authentication authentication,
                                         HttpServletRequest request,
                                         DeviceTrustLevel requiredLevel) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Authentication missing or not authenticated");
            return false;
        }

        if (requiredLevel == null) {
            log.debug("Required trust level is null, allowing access");
            return true;
        }

        Device device = getAuthenticatedDevice(request);

        if (device == null) {
            log.debug("No authenticated device found for trust level check");
            return false;
        }

        DeviceTrustLevel currentLevel = device.getDeviceTrustLevel();
        if (currentLevel == null) {
            log.debug("Device {} has null trust level", device.getId());
            return false;
        }

        boolean hasRequiredLevel = currentLevel.ordinal() >= requiredLevel.ordinal();

        log.debug("Trust level check for device {}: current={}, required={}, result={}",
                device.getId(), currentLevel, requiredLevel, hasRequiredLevel);

        return hasRequiredLevel;
    }

    /**
     * Checks if the current device is confirmed by the user.
     * Confirmed devices are those that the user has explicitly validated.
     *
     * @param authentication Current authentication context
     * @param request HTTP request containing device information
     * @return true if device is confirmed, false otherwise
     */
    public boolean isDeviceConfirmed(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Device device = getAuthenticatedDevice(request);
        return device != null && device.isConfirmed();
    }

    /**
     * Checks if the current device is blacklisted.
     * Blacklisted devices should be denied access to all protected resources.
     *
     * @param authentication Current authentication context
     * @param request HTTP request containing device information
     * @return true if device is blacklisted, false otherwise
     */
    public boolean isDeviceBlacklisted(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Device device = getAuthenticatedDevice(request);
        return device != null && device.isBlacklisted();
    }

    /**
     * Gets the current device trust level.
     *
     * @param authentication Current authentication context
     * @param request HTTP request containing device information
     * @return Current device trust level or null if not available
     */
    public DeviceTrustLevel getCurrentDeviceTrustLevel(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Device device = getAuthenticatedDevice(request);
        return device != null ? device.getDeviceTrustLevel() : null;
    }

    /**
     * Checks if the current device meets multiple security criteria.
     * This is a convenience method for complex authorization rules.
     *
     * @param authentication Current authentication context
     * @param request HTTP request containing device information
     * @param requiredLevel Minimum trust level required
     * @param mustBeConfirmed Whether device must be confirmed
     * @param allowBlacklisted Whether blacklisted devices are allowed (usually false)
     * @return true if all criteria are met, false otherwise
     */
    public boolean meetsSecurityCriteria(Authentication authentication,
                                         HttpServletRequest request,
                                         DeviceTrustLevel requiredLevel,
                                         boolean mustBeConfirmed,
                                         boolean allowBlacklisted) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Device device = getAuthenticatedDevice(request);
        if (device == null) {
            return false;
        }

        // Check blacklist status
        if (!allowBlacklisted && device.isBlacklisted()) {
            log.debug("Device {} is blacklisted, denying access", device.getId());
            return false;
        }

        // Check confirmation status
        if (mustBeConfirmed && !device.isConfirmed()) {
            log.debug("Device {} is not confirmed, denying access", device.getId());
            return false;
        }

        // Check trust level
        if (requiredLevel != null) {
            DeviceTrustLevel currentLevel = device.getDeviceTrustLevel();
            if (currentLevel == null || currentLevel.ordinal() < requiredLevel.ordinal()) {
                log.debug("Device {} trust level {} insufficient, required {}",
                        device.getId(), currentLevel, requiredLevel);
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves the authenticated device from the current request context.
     * This method tries multiple approaches to ensure device information is available.
     *
     * @param request HTTP request containing device information
     * @return Authenticated device or null if not available
     */
    private Device getAuthenticatedDevice(HttpServletRequest request) {
        // 1. Try to get device from request attributes (fastest)
        Device device = DeviceContextProvider.getAuthenticatedDevice(request);

        if (device != null) {
            log.debug("Device {} retrieved from request context", device.getId());
            return device;
        }

        // 2. Fallback: get device via JWT claims and security service
        Claims claims = (Claims) request.getAttribute("jwt_claims");
        if (claims != null) {
            Long deviceId = claims.get("deviceId", Long.class);
            String fingerprint = claims.get("deviceFingerprint", String.class);

            if (deviceId != null && fingerprint != null) {
                DeviceSecurityResult result = deviceSecurityService
                        .getSecureDeviceFromCacheOrDatabase(deviceId, fingerprint);

                if (result.isSuccess()) {
                    device = result.getDevice();
                    log.debug("Device {} retrieved via security service fallback", deviceId);

                    // Store in request context for future use
                    DeviceContextProvider.setAuthenticatedDevice(request, device);
                    return device;
                } else {
                    log.warn("Failed to retrieve device {} via security service: {}",
                            deviceId, result.getErrorMessage());
                }
            }
        }

        log.debug("No authenticated device found in request");
        return null;
    }
}