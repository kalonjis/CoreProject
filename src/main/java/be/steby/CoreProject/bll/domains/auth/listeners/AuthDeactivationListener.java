package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.common.services.tokens.GlobalTokenServiceImpl;
import be.steby.CoreProject.bll.domains.account.events.UserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener that handles automatic logout when a user account is deactivated.
 * This ensures that deactivated users are immediately logged out from all devices
 * by revoking all their tokens regardless of type.
 *
 * Security Process:
 * 1. Revokes ALL tokens for the user (refresh, device confirmation, password reset, etc.)
 * 2. Publishes UserLogoutEvent to trigger cache invalidation
 *
 * This listener executes before notification listeners to ensure security operations
 * complete before sending confirmation emails.
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class AuthDeactivationListener {

    private final GlobalTokenServiceImpl globalTokenService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Automatically logs out a user when their account is deactivated.
     * Performs the same security operations as AuthService.logout() but across all devices:
     * 1. Revokes all tokens for the user (all types: refresh, device, password reset, etc.)
     * 2. Disconnect all devices where user was connected
     * 3. Publishes UserLogoutEvent for cache clearing
     *
     * @param event the user deactivation event containing the deactivated user
     */
    @EventListener
    @Async("generalPurposeExecutor")
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        User user = event.user();

        log.info("Automatically logging out deactivated user: {}", user.getUsername());

        try {
            // Revoke ALL tokens for this user (all types)
            int revokedCount = globalTokenService.revokeAllUserTokens(user.getId());
            log.debug("Revoked {} tokens for deactivated user: {}", revokedCount, user.getUsername());

            int disconnectedDevice = deviceService.disconnectAllDevicesForUser(user);

            // Publish UserLogoutEvent to trigger cache clearing
            // device=null because we're forcing logout from all devices
            eventPublisher.publishEvent(new UserLogoutEvent(user, null));

            log.info("Automatic logout completed for deactivated user: {} ({} tokens revoked)",
                    user.getUsername(), revokedCount);

        } catch (Exception e) {
            log.error("Error during automatic logout for deactivated user {}: {}",
                    user.getUsername(), e.getMessage(), e);
        }
    }
}