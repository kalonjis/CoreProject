package be.steby.CoreProject.bll.domains.device.listeners;

import be.steby.CoreProject.bll.domains.device.events.DeviceSecurityEvent;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.device.services.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Listener responsible for handling device security events.
 * This listener determines when and how to send device security-related notifications to users.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DeviceNotificationListener {

    private final MailerService mailerService;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;

    @EventListener
    @Async("emailExecutor")
    public void handleDeviceSecurityEvent(DeviceSecurityEvent event) {
        log.info("Processing device security events: {} for user {}",
                event.type(), event.user().getUsername());

        switch (event.type()) {
            case UNCONFIRMED_DEVICE -> handleUnconfirmedDevice(event);
            case BLACKLISTED_DEVICE_ATTEMPT -> handleBlacklistedDevice(event);
            case NEW_DEVICE_DETECTED -> handleNewDevice(event);
            case SUSPICIOUS_LOGIN -> handleSuspiciousLogin(event);
        }
    }

    /**
     * Handles notifications for unconfirmed devices.
     * Determines if a notification should be sent based on device and user context.
     */
    private void handleUnconfirmedDevice(DeviceSecurityEvent event) {
        boolean shouldNotify = shouldSendNotificationForUnconfirmedDevice(event);

        if (shouldNotify) {
            try {
                DeviceConfirmationToken token = deviceConfirmationTokenService.createDeviceConfirmationToken(
                        event.user(), event.device().getId());

                if (event.device().isBlacklisted()) {
                    mailerService.sendBlacklistedDeviceAlert(
                            event.user(), event.device(), token.getPublicId());
                    log.info("Blacklisted device alert sent for device: {}", event.device().getId());
                } else {
                    mailerService.sendNewDeviceAlert(
                            event.user(), event.device(), token.getPublicId());
                    log.info("New device alert sent for device: {}", event.device().getId());
                }
            } catch (MaxAttemptsReachedException e) {
                log.warn("Device confirmation email skipped - max attempts reached for user {} device {}",
                        event.user().getUsername(), event.device().getId());
            }
        } else {
            log.info("Device security notification skipped for unconfirmed device: {}", event.device().getId());
        }
    }

    /**
     * Handles notifications for blacklisted device attempts.
     */
    private void handleBlacklistedDevice(DeviceSecurityEvent event) {
        try {
            DeviceConfirmationToken token = deviceConfirmationTokenService.createDeviceConfirmationToken(
                    event.user(), event.device().getId());

            mailerService.sendBlacklistedDeviceAlert(
                    event.user(), event.device(), token.getPublicId());

            log.info("Blacklisted device attempt notification sent for user: {}", event.user().getUsername());
        } catch (MaxAttemptsReachedException e) {
            log.warn("Blacklisted device alert skipped - max attempts reached for user {} device {}",
                    event.user().getUsername(), event.device().getId());
        }
    }

    /**
     * Handles notifications for newly detected devices.
     */
    private void handleNewDevice(DeviceSecurityEvent event) {
        try {
            DeviceConfirmationToken token = deviceConfirmationTokenService.createDeviceConfirmationToken(
                    event.user(), event.device().getId());

            mailerService.sendNewDeviceAlert(
                    event.user(), event.device(), token.getPublicId());

            log.info("New device detection notification sent for user: {}", event.user().getUsername());
        } catch (MaxAttemptsReachedException e) {
            log.warn("New device alert skipped - max attempts reached for user {} device {}",
                    event.user().getUsername(), event.device().getId());
        }
    }

    /**
     * Handles notifications for suspicious login activities.
     */
    private void handleSuspiciousLogin(DeviceSecurityEvent event) {
        // Implementation for suspicious login notifications
        log.info("Suspicious login notification would be sent for user: {}", event.user().getUsername());
    }

    /**
     * Determines whether a notification should be sent for an unconfirmed device.
     *
     * @param event The device security events
     * @return true if a notification should be sent, false otherwise
     */
    private boolean shouldSendNotificationForUnconfirmedDevice(DeviceSecurityEvent event) {
        Device device = event.device();
        User user = event.user();

        // Always notify if device is blacklisted
        if (device.isBlacklisted()) {
            log.info("Notification required - device is blacklisted");
            return true;
        }

        // Skip notification if first device and account activated recently (< 10 minutes)
        if (device.isFirstDeviceUsed() && user.getActivatedAt() != null) {
            long minutesSinceActivation = Duration.between(
                    user.getActivatedAt(), Instant.now()).toMinutes();

            if (minutesSinceActivation <= 10) {
                log.info("Notification skipped - recent account activation ({}min ago)", minutesSinceActivation);
                return false;
            }
        }

        // Send notification for all other cases
        log.info("Notification required - unconfirmed device login");
        return true;
    }
}