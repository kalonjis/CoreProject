package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLoginFailedEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.domains.auth.services.activity_log.AuthActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for authentication-related events
 * Handles activity logging for auth domain events
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class AuthActivityListener {

    private final AuthActivityLogService authActivityLogService;

    /**
     * Handle successful user login events
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserLoggedIn(UserLoggedInEvent event) {
        try {
            log.debug("Processing successful login event for user: {}", event.user().getUsername());

            authActivityLogService.logSuccessfulLogin(event);

            log.debug("Successful login logged for user: {}", event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to log successful login for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Handle failed login attempts for known users
     * Important for security monitoring
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserLoginFailed(UserLoginFailedEvent event) {
        try {
            log.debug("Processing failed login event for user: {}", event.user().getUsername());

            authActivityLogService.logFailedLogin(event);

            log.debug("Failed login logged for user: {}", event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to log failed login for user: {}", event.user().getUsername(), e);
        }
    }

    /**
     * Handle user logout events
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserLoggedOut(UserLogoutEvent event) {
        try {
            log.debug("Processing logout event for user: {}", event.user().getUsername());

            authActivityLogService.logUserLogout(event);

            log.debug("Logout logged for user: {}", event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to log logout for user: {}", event.user().getUsername(), e);
        }
    }
}