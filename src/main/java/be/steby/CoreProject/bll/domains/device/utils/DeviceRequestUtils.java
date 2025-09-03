package be.steby.CoreProject.bll.domains.device.utils;

import be.steby.CoreProject.dl.entities.Device;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for managing device information in HTTP requests.
 * Provides consistent access to device data across the application.
 */
@Slf4j
public class DeviceRequestUtils {

    public static final String CURRENT_DEVICE_ATTRIBUTE = "currentDevice";
    public static final String JWT_CLAIMS_ATTRIBUTE = "jwt_claims";

    /**
     * Store device in request attributes for reuse within the same request
     * @param request The HTTP request
     * @param device The device to store
     */
    public static void setCurrentDevice(HttpServletRequest request, Device device) {
        if (request != null && device != null) {
            request.setAttribute(CURRENT_DEVICE_ATTRIBUTE, device);
            log.debug("Stored device {} in request attributes", device.getId());
        }
    }

    /**
     * Retrieve device from request attributes
     * @param request The HTTP request
     * @return The device or null if not found
     */
    public static Device getCurrentDevice(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Object deviceAttr = request.getAttribute(CURRENT_DEVICE_ATTRIBUTE);
        if (deviceAttr instanceof Device) {
            return (Device) deviceAttr;
        }

        return null;
    }

    /**
     * Get device ID from JWT claims in request
     * @param request The HTTP request
     * @return The device ID or null if not found
     */
    public static Long getDeviceIdFromClaims(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Claims claims = (Claims) request.getAttribute(JWT_CLAIMS_ATTRIBUTE);
        if (claims != null) {
            return claims.get("deviceId", Long.class);
        }

        return null;
    }

    /**
     * Get JWT claims from request
     * @param request The HTTP request
     * @return The claims or null if not found
     */
    public static Claims getJwtClaims(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Object claimsAttr = request.getAttribute(JWT_CLAIMS_ATTRIBUTE);
        if (claimsAttr instanceof Claims) {
            return (Claims) claimsAttr;
        }

        return null;
    }

    /**
     * Check if device information is available in request
     * @param request The HTTP request
     * @return true if device is available, false otherwise
     */
    public static boolean hasCurrentDevice(HttpServletRequest request) {
        return getCurrentDevice(request) != null;
    }

    /**
     * Get device fingerprint from JWT claims
     * @param request The HTTP request
     * @return The device fingerprint or null if not found
     */
    public static String getDeviceFingerprintFromClaims(HttpServletRequest request) {
        Claims claims = getJwtClaims(request);
        if (claims != null) {
            return claims.get("deviceFingerprint", String.class);
        }
        return null;
    }

    /**
     * Check if device is confirmed according to JWT claims
     * @param request The HTTP request
     * @return true if confirmed, false otherwise
     */
    public static boolean isDeviceConfirmedFromClaims(HttpServletRequest request) {
        Claims claims = getJwtClaims(request);
        if (claims != null) {
            Boolean confirmed = claims.get("deviceConfirmed", Boolean.class);
            return Boolean.TRUE.equals(confirmed);
        }
        return false;
    }
}