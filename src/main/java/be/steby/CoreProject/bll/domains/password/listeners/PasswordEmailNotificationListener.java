package be.steby.CoreProject.bll.domains.password.listeners;

import be.steby.CoreProject.bll.domains.password.events.email.PasswordChangeEmailNotificationEvent;
import be.steby.CoreProject.bll.domains.password.events.email.PasswordResetEmailRequestedEvent;
import be.steby.CoreProject.bll.domains.password.events.email.PasswordResetTokenRefreshRequestedEvent;
import be.steby.CoreProject.bll.domains.password.services.notification.PasswordMailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Event listener for password-related email notifications.
 * 
 * <p>This service listens to password domain events that require email notifications
 * and delegates to the PasswordMailerService for actual email sending. All email
 * operations are asynchronous to prevent blocking the main password operation flow.
 * 
 * <p>Responsibility: EMAIL notifications only
 * 
 * <p>Handled events:
 * <ul>
 *   <li>PasswordResetEmailRequestedEvent - Sends password reset email with link</li>
 *   <li>PasswordResetTokenRefreshRequestedEvent - Sends new token when previous expired</li>
 *   <li>PasswordChangeEmailNotificationEvent - Sends confirmation email for security</li>
 *   <li>SmsPasswordResetEmailNotificationEvent - Cross-channel notification</li>
 * </ul>
 * 
 * <p>Email delivery failures are logged but do not affect the core password operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordEmailNotificationListener {

    private final PasswordMailerService passwordMailerService;

    /**
     * Handles password reset request events for email delivery.
     * 
     * @param event the password reset email request event
     */
    @EventListener
    @Async("emailExecutor")
    public void handlePasswordResetEmailRequested(PasswordResetEmailRequestedEvent event) {
        log.debug("Handling password reset email request for user: {}", event.user().getUsername());
        
        try {
            passwordMailerService.sendPasswordReset(event.tokenPublicId(), event.user());
            log.debug("Password reset email triggered successfully for user: {}", event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password reset email for user: {} - error: {}", 
                     event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - email failure should not affect password reset process
        }
    }

    /**
     * Handles password reset token refresh request events.
     * 
     * @param event the password reset token refresh request event
     */
    @EventListener
    @Async("emailExecutor")
    public void handlePasswordResetTokenRefreshRequested(PasswordResetTokenRefreshRequestedEvent event) {
        log.debug("Handling password reset token refresh for user: {}", event.user().getUsername());
        
        try {
            passwordMailerService.sendPasswordResetRefresh(event.newTokenPublicId(), event.user());
            log.debug("Password reset token refresh email triggered successfully for user: {}", 
                     event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password reset token refresh email for user: {} - error: {}", 
                     event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - email failure should not affect token refresh process
        }
    }

    /**
     * Handles password change email notification events.
     * 
     * @param event the password change email notification event
     */
    @EventListener
    @Async("emailExecutor")
    public void handlePasswordChangeEmailNotification(PasswordChangeEmailNotificationEvent event) {
        log.debug("Handling password change email notification for user: {}", event.user().getUsername());
        
        try {
            passwordMailerService.sendPasswordChangeConfirmation(event.user());
            log.debug("Password change email notification triggered successfully for user: {}", 
                     event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password change email notification for user: {} - error: {}", 
                     event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - email failure should not affect password change process
        }
    }

    /**
     * Handles SMS password reset email notification events.
     * 
     * <p>This is a cross-channel notification: when a user requests password reset
     * via SMS, we also send an email notification for security awareness.
     * 
     * @param event the SMS password reset email notification event
     */
//    @EventListener
//    @Async("emailExecutor")
//    public void handleSmsPasswordResetEmailNotification(SmsPasswordResetEmailNotificationEvent event) {
//        log.debug("Handling SMS password reset email notification for user: {}", event.user().getUsername());
//
//        try {
//            passwordMailerService.sendSmsPasswordResetNotification(event.user(), event.maskedPhoneNumber());
//            log.debug("SMS password reset email notification triggered successfully for user: {}",
//                     event.user().getUsername());
//        } catch (Exception e) {
//            log.error("Failed to send SMS password reset email notification for user: {} - error: {}",
//                     event.user().getUsername(), e.getMessage(), e);
//            // Don't rethrow - email failure should not affect SMS reset process
//        }
//    }
}