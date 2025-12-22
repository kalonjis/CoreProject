package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.services.mailer.BaseMailerService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.MailerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

/**
 * Device domain-specific mailer service.
 * Extends BaseMailerService to inherit common email utilities.
 * Handles all email notifications related to device operations:
 * - New device login alerts
 * - Blacklisted device attempt notifications
 * - Device confirmation requests
 * - Device security warnings
 *
 * All device-related email operations are designed to be non-blocking and failure-tolerant.
 * Security notifications should not affect the core authentication flow.
 */
@Service
@Slf4j
public class DeviceMailerService extends BaseMailerService {

    public DeviceMailerService(MailerUtil mailerUtil) {
        super(mailerUtil);
    }

    /**
     * Sends new device login alert email to user.
     * This notification is triggered when a user logs in from an unconfirmed device.
     * The email includes device information and links to confirm or reject the device.
     *
     * @param user The user who logged in
     * @param device The device used for login
     * @param tokenPublicId The public ID of the device confirmation token
     */
    public void sendNewDeviceAlert(User user, Device device, String tokenPublicId) {
        log.info("Sending new device alert email to: {} for device: {}",
                user.getEmail(), device.getId());

        String confirmDeviceUrl = buildUrl("/auth/device-confirmation",
                "token", tokenPublicId,
                "action", "confirm");
        String rejectDeviceUrl = buildUrl("/auth/device-confirmation",
                "token", tokenPublicId,
                "action", "reject");

        Context context = createBaseContext(user);
        context.setVariable("deviceType", formatDeviceType(device));
        context.setVariable("confirmDeviceUrl", confirmDeviceUrl);
        context.setVariable("revokeDeviceUrl", rejectDeviceUrl);
        context.setVariable("location", formatLocation(device));
        context.setVariable("timestamp", formatDate(device.getLastSeen()));
        context.setVariable("token", tokenPublicId);
        context.setVariable("deviceBrowser", formatBrowser(device));
        context.setVariable("deviceOS", formatOperatingSystem(device));
        context.setVariable("ipAddress", device.getLastIpAddress());

        sendEmail("Security Alert: New Device Login", "devices/newDeviceAlert", context, user.getEmail());

        log.debug("New device alert email sent successfully to: {} for device: {}",
                user.getEmail(), device.getId());
    }

    /**
     * Sends blacklisted device login attempt alert email to user.
     * This notification is triggered when someone attempts to login from a blacklisted device.
     * The email includes device information and a link to whitelist the device if authorized.
     *
     * @param user The user whose account was accessed
     * @param device The blacklisted device used for login attempt
     * @param tokenPublicId The public ID of the device confirmation token
     */
    public void sendBlacklistedDeviceAlert(User user, Device device, String tokenPublicId) {
        log.info("Sending blacklisted device alert email to: {} for device: {}",
                user.getEmail(), device.getId());

        String whitelistDeviceUrl = buildUrl("/auth/device-confirmation",
                "token", tokenPublicId,
                "action", "confirm");

        Context context = createBaseContext(user);
        context.setVariable("deviceType", formatDeviceType(device));
        context.setVariable("whitelistDeviceUrl", whitelistDeviceUrl);
        context.setVariable("location", formatLocation(device));
        context.setVariable("timestamp", formatDate(device.getLastSeen()));
        context.setVariable("token", tokenPublicId);
        context.setVariable("deviceBrowser", formatBrowser(device));
        context.setVariable("deviceOS", formatOperatingSystem(device));
        context.setVariable("ipAddress", device.getLastIpAddress());
        context.setVariable("blacklistReason", "Previous security concerns");

        sendEmail("Security Alert: Blacklisted Device Login Attempt",
                "devices/blacklistedDeviceAlert", context, user.getEmail());

        log.debug("Blacklisted device alert email sent successfully to: {} for device: {}",
                user.getEmail(), device.getId());
    }

    /**
     * Sends device confirmation reminder email to user.
     * This notification is sent when a device remains unconfirmed after a certain period.
     *
     * @param user The user who needs to confirm the device
     * @param device The unconfirmed device
     * @param tokenPublicId The public ID of the device confirmation token
     */
    public void sendDeviceConfirmationReminder(User user, Device device, String tokenPublicId) {
        log.info("Sending device confirmation reminder email to: {} for device: {}",
                user.getEmail(), device.getId());

        String confirmDeviceUrl = buildUrl("/auth/device-confirmation",
                "token", tokenPublicId,
                "action", "confirm");
        String rejectDeviceUrl = buildUrl("/auth/device-confirmation",
                "token", tokenPublicId,
                "action", "reject");

        Context context = createBaseContext(user);
        context.setVariable("deviceType", formatDeviceType(device));
        context.setVariable("confirmDeviceUrl", confirmDeviceUrl);
        context.setVariable("rejectDeviceUrl", rejectDeviceUrl);
        context.setVariable("location", formatLocation(device));
        context.setVariable("firstSeenTimestamp", formatDate(device.getFirstSeen()));
        context.setVariable("tokenPublicId", tokenPublicId);

        sendEmail("Device Confirmation Reminder", "devices/deviceConfirmationReminder", context, user.getEmail());

        log.debug("Device confirmation reminder email sent successfully to: {} for device: {}",
                user.getEmail(), device.getId());
    }

    /**
     * Sends device revocation confirmation email to user.
     * This notification is sent when a device has been successfully revoked.
     *
     * @param user The user whose device was revoked
     * @param device The revoked device
     */
    public void sendDeviceRevocationConfirmation(User user, Device device) {
        log.info("Sending device revocation confirmation email to: {} for device: {}",
                user.getEmail(), device.getId());

        Context context = createBaseContext(user);
        context.setVariable("deviceType", formatDeviceType(device));
        context.setVariable("location", formatLocation(device));
        context.setVariable("revocationTimestamp", formatDate(device.getUpdatedAt()));
        context.setVariable("deviceBrowser", formatBrowser(device));
        context.setVariable("deviceOS", formatOperatingSystem(device));

        sendEmail("Device Access Revoked", "devices/deviceRevocationConfirmation", context, user.getEmail());

        log.debug("Device revocation confirmation email sent successfully to: {} for device: {}",
                user.getEmail(), device.getId());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Formats device type for display in emails.
     * Uses the device's own getName() method which provides better formatting.
     *
     * @param device The device to format
     * @return Formatted device type string
     */
    private String formatDeviceType(Device device) {
        // Use the device's built-in getName() method which handles all the logic
        String deviceName = device.getName();

        // Fallback if getName() returns null or empty
        if (deviceName == null || deviceName.trim().isEmpty()) {
            return "Unknown Device";
        }

        return deviceName;
    }

    /**
     * Formats browser information for display in emails.
     * Combines browser name and version if available.
     *
     * @param device The device to format
     * @return Formatted browser string
     */
    private String formatBrowser(Device device) {
        if (device.getBrowser() == null || device.getBrowser().isBlank()) {
            return "Unknown Browser";
        }

        String browser = device.getBrowser();
        if (device.getBrowserVersion() != null && !device.getBrowserVersion().isBlank()) {
            browser += " " + device.getBrowserVersion();
        }

        return browser;
    }

    /**
     * Formats operating system information for display in emails.
     * Combines OS name and version if available.
     *
     * @param device The device to format
     * @return Formatted operating system string
     */
    private String formatOperatingSystem(Device device) {
        if (device.getOperatingSystem() == null || device.getOperatingSystem().isBlank()) {
            return "Unknown OS";
        }

        String os = device.getOperatingSystem();
        if (device.getOsVersion() != null && !device.getOsVersion().isBlank()) {
            os += " " + device.getOsVersion();
        }

        return os;
    }

    /**
     * Formats location information for display in emails.
     * Provides fallback for unknown locations.
     *
     * @param device The device to format
     * @return Formatted location string
     */
    private String formatLocation(Device device) {
        if (device.getLocation() == null || device.getLocation().isBlank()) {
            return "Unknown Location";
        }
        return device.getLocation();
    }
}