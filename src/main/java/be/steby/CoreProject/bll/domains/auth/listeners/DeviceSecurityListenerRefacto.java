//package be.steby.CoreProject.bll.domains.auth.listeners;
//
//import be.steby.CoreProject.bll.domains.device.events.DeviceSecurityEvent;
//import be.steby.CoreProject.bll.common.services.mailer.MailerService;
//import be.steby.CoreProject.bll.domains.device.services.DeviceConfirmationTokenServiceImpl;
//import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
//import be.steby.CoreProject.dl.entities.Device;
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.EventListener;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.function.Consumer;
//
///**
// * Listener responsible for handling device security events.
// * This listener determines when and how to send device security-related notifications to users.
// */
//@Component
//@Order(10)
//@RequiredArgsConstructor
//@Slf4j
//public class DeviceSecurityListenerRefacto {
//
//    private final MailerService mailerService;
//    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;
//
//    @EventListener
//    @Async("emailExecutor")
//    public void handleDeviceSecurityEvent(DeviceSecurityEvent event) {
//        log.info("Processing device security event: {} for user {}",
//                event.type(), event.user().getUsername());
//
//        switch (event.type()) {
//            case UNCONFIRMED_DEVICE -> handleUnconfirmedDevice(event);
//            case BLACKLISTED_DEVICE_ATTEMPT -> handleWithTokenAndEmail(event, this::sendBlacklistedAlert, "Blacklisted device attempt");
//            case NEW_DEVICE_DETECTED -> handleWithTokenAndEmail(event, this::sendNewDeviceAlert, "New device detection");
//            case SUSPICIOUS_LOGIN -> handleSuspiciousLogin(event);
//        }
//    }
//
//    /**
//     * Handles notifications for unconfirmed devices with conditional logic.
//     */
//    private void handleUnconfirmedDevice(DeviceSecurityEvent event) {
//        if (!shouldSendNotificationForUnconfirmedDevice(event)) {
//            log.info("Device security notification skipped for unconfirmed device: {}", event.device().getId());
//            return;
//        }
//
//        Consumer<DeviceEmailContext> emailSender = event.device().isBlacklisted()
//                ? this::sendBlacklistedAlert
//                : this::sendNewDeviceAlert;
//
//        String actionType = event.device().isBlacklisted() ? "Blacklisted device alert" : "New device alert";
//
//        handleWithTokenAndEmail(event, emailSender, actionType);
//    }
//
//    /**
//     * Handles notifications for suspicious login activities.
//     */
//    private void handleSuspiciousLogin(DeviceSecurityEvent event) {
//        // TODO: Implement suspicious login notifications
//        log.info("Suspicious login notification would be sent for user: {}", event.user().getUsername());
//    }
//
//    /**
//     * Generic handler for events that require token creation and email sending.
//     */
//    private void handleWithTokenAndEmail(DeviceSecurityEvent event,
//                                         Consumer<DeviceEmailContext> emailSender,
//                                         String actionDescription) {
//        try {
//            DeviceConfirmationToken token = createTokenForDevice(event.user(), event.device());
//            DeviceEmailContext context = new DeviceEmailContext(event.user(), event.device(), token.getPublicId());
//
//            emailSender.accept(context);
//            log.info("{} notification sent for user: {}", actionDescription, event.user().getUsername());
//
//        } catch (MaxAttemptsReachedException e) {
//            log.warn("{} skipped - max attempts reached for user {} device {}",
//                    actionDescription, event.user().getUsername(), event.device().getId());
//        }
//    }
//
//    /**
//     * Creates a device confirmation token with error handling.
//     */
//    private DeviceConfirmationToken createTokenForDevice(User user, Device device) throws MaxAttemptsReachedException {
//        return deviceConfirmationTokenService.createDeviceConfirmationToken(user, device.getId());
//    }
//
//    /**
//     * Sends blacklisted device alert email.
//     */
//    private void sendBlacklistedAlert(DeviceEmailContext context) {
//        mailerService.sendBlacklistedDeviceAlert(context.user(), context.device(), context.tokenId());
//    }
//
//    /**
//     * Sends new device alert email.
//     */
//    private void sendNewDeviceAlert(DeviceEmailContext context) {
//        mailerService.sendNewDeviceAlert(context.user(), context.device(), context.tokenId());
//    }
//
//    /**
//     * Determines whether a notification should be sent for an unconfirmed device.
//     */
//    private boolean shouldSendNotificationForUnconfirmedDevice(DeviceSecurityEvent event) {
//        Device device = event.device();
//        User user = event.user();
//
//        // Always notify if device is blacklisted
//        if (device.isBlacklisted()) {
//            log.info("Notification required - device is blacklisted");
//            return true;
//        }
//
//        // Skip notification if first device and account activated recently (< 10 minutes)
//        if (isRecentlyActivatedFirstDevice(device, user)) {
//            long minutesSinceActivation = getMinutesSinceActivation(user);
//            log.info("Notification skipped - recent account activation ({}min ago)", minutesSinceActivation);
//            return false;
//        }
//
//        // Send notification for all other cases
//        log.info("Notification required - unconfirmed device login");
//        return true;
//    }
//
//    /**
//     * Checks if this is a first device for a recently activated account.
//     */
//    private boolean isRecentlyActivatedFirstDevice(Device device, User user) {
//        return device.isFirstDeviceUsed()
//                && user.getActivatedAt() != null
//                && getMinutesSinceActivation(user) <= 10;
//    }
//
//    /**
//     * Calculates minutes since account activation.
//     */
//    private long getMinutesSinceActivation(User user) {
//        return Duration.between(user.getActivatedAt(), Instant.now()).toMinutes();
//    }
//
//    /**
//     * Context record for email operations to avoid parameter repetition.
//     */
//    private record DeviceEmailContext(User user, Device device, String tokenId) {}
//}