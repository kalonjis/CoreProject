package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.services.AuthActivityLogService;
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
     * Handle user login events and log the activity
     * Device is already provided in the event from AuthServiceImpl
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleUserLoggedIn(UserLoggedInEvent event) {
        try {
            log.debug("Processing login event for user: {}", event.user().getUsername());

            authActivityLogService.logUserLogin(event);

            log.debug("Login activity logged successfully for user: {}", event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to log login activity for user: {}", event.user().getUsername(), e);
        }
    }
}