package be.steby.CoreProject.bll.domains.device.utils;

import be.steby.CoreProject.dl.entities.Device;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for retrieving authenticated device from request context.
 * Provides a clean abstraction for accessing device information set by JwtFilter.
 */
public class DeviceContextProvider {

    private static final String AUTHENTICATED_DEVICE_ATTRIBUTE = "AUTHENTICATED_DEVICE";

    /**
     * Retrieves the authenticated device from the current request.
     * This device has been validated and cached by the JwtFilter.
     *
     * @param request Current HTTP request
     * @return Authenticated device or null if not available
     */
    public static Device getAuthenticatedDevice(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Object deviceAttr = request.getAttribute(AUTHENTICATED_DEVICE_ATTRIBUTE);
        return deviceAttr instanceof Device ? (Device) deviceAttr : null;
    }

    /**
     * Checks if an authenticated device is available in the current request.
     *
     * @param request Current HTTP request
     * @return true if authenticated device is present, false otherwise
     */
    public static boolean hasAuthenticatedDevice(HttpServletRequest request) {
        return getAuthenticatedDevice(request) != null;
    }

    /**
     * Sets the authenticated device in the request attributes.
     * Should only be called by security components like JwtFilter.
     *
     * @param request Current HTTP request
     * @param device Authenticated device to store
     */
    public static void setAuthenticatedDevice(HttpServletRequest request, Device device) {
        if (request != null) {
            request.setAttribute(AUTHENTICATED_DEVICE_ATTRIBUTE, device);
        }
    }
}