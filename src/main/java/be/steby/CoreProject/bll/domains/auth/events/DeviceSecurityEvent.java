package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event emitted when a device security issue is detected and requires user notification.
 * This event is used to trigger appropriate security alerts based on device-related scenarios.
 */
public record DeviceSecurityEvent(
        User user,
        Device device,
        DeviceSecurityType type,
        RequestContext requestContext
) {

    /**
     * Types of device security issues that can trigger notifications.
     */
    public enum DeviceSecurityType {
        /**
         * Device used for login is not confirmed by the user.
         */
        UNCONFIRMED_DEVICE,

        /**
         * Attempt to login with a blacklisted device.
         */
        BLACKLISTED_DEVICE_ATTEMPT,

        /**
         * Suspicious login activity detected from this device.
         */
        SUSPICIOUS_LOGIN,

        /**
         * New device detected for the user.
         */
        NEW_DEVICE_DETECTED
    }
}