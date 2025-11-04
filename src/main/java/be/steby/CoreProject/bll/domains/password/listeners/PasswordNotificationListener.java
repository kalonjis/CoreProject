package be.steby.CoreProject.bll.domains.password.listeners;

import be.steby.CoreProject.bll.domains.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordTokenEvent;
import be.steby.CoreProject.bll.domains.password.services.PasswordMailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Event listener for password-related notifications.
 *
 * <p>This service listens to password domain events and triggers appropriate
 * email notifications through the PasswordMailerService. All email operations
 * are asynchronous to prevent blocking the main password operation flow.
 *
 * <p>Handled events:
 * <ul>
 *   <li>RequestPasswordResetEvent - Sends password reset email with link</li>
 *   <li>RequestPasswordTokenEvent - Sends new token when previous expired</li>
 *   <li>PasswordChangedEvent - Sends confirmation email for security</li>
 * </ul>
 *
 * <p>Email delivery failures are logged but do not affect the core password operations.
 * This ensures that password functionality remains available even if email service
 * is temporarily unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordNotificationListener {

    private final PasswordMailerService passwordMailerService;

    /**
     * Handles password reset request events.
     *
     * <p>Triggered when a user requests a password reset. Sends an email
     * containing a secure reset link that expires after a configured period.
     *
     * @param event the password reset request event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleRequestPasswordReset(RequestPasswordResetEvent event) {
        log.debug("Handling password reset request event for user: {}", event.user().getUsername());

        try {
            passwordMailerService.sendPasswordReset(event.token(), event.user());
            log.debug("Password reset email triggered successfully for user: {}", event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password reset email for user: {} - error: {}",
                    event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - email failure should not affect password reset process
        }
    }

    /**
     * Handles password change confirmation events.
     *
     * <p>Triggered when a user successfully changes their password. Sends a
     * security confirmation email to notify the user of the password change.
     * This helps detect unauthorized password changes.
     *
     * @param event the password changed event
     */
    @EventListener
    @Async("emailExecutor")
    public void handlePasswordChanged(PasswordChangedEvent event) {
        log.debug("Handling password changed event for user: {}", event.user().getUsername());

        try {
            passwordMailerService.sendPasswordChangeConfirmation(event.user());
            log.debug("Password change confirmation email triggered successfully for user: {}",
                    event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email for user: {} - error: {}",
                    event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - email failure should not affect password change process
        }
    }

    /**
     * Handles password token refresh request events.
     *
     * <p>Triggered when a user requests a new password reset token because
     * their previous token expired. Sends a new reset email with a fresh token
     * while invalidating the old one.
     *
     * @param event the password token refresh request event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleRequestPasswordToken(RequestPasswordTokenEvent event) {
        log.debug("Handling password token refresh event for user: {}", event.user().getUsername());

        try {
            passwordMailerService.sendPasswordResetRefresh(event.newToken(), event.user());
            log.debug("Password token refresh email triggered successfully for user: {}",
                    event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password token refresh email for user: {} - error: {}",
                    event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - email failure should not affect token refresh process
        }
    }
}