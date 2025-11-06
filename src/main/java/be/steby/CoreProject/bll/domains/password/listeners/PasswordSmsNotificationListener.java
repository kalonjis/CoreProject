package be.steby.CoreProject.bll.domains.password.listeners;

import be.steby.CoreProject.bll.domains.password.events.sms.PasswordChangeSmsAlertEvent;
import be.steby.CoreProject.bll.domains.password.events.sms.PasswordResetSmsRequestedEvent;
import be.steby.CoreProject.bll.domains.password.services.notification.PasswordSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Event listener for password-related SMS notifications.
 * 
 * <p>This service listens to password domain events that require SMS notifications
 * and delegates to the PasswordSmsNotificationService for actual SMS sending. All SMS
 * operations are asynchronous to prevent blocking the main password operation flow.
 * 
 * <p>Responsibility: SMS notifications only
 * 
 * <p>Handled events:
 * <ul>
 *   <li>PasswordResetSmsRequestedEvent - Generates and sends 6-digit verification code</li>
 *   <li>PasswordChangeSmsAlertEvent - Sends security alert for password changes</li>
 * </ul>
 * 
 * <p>SMS delivery failures are logged but do not affect the core password operations.
 * If a user doesn't have a verified phone number, SMS operations are silently skipped.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordSmsNotificationListener {

    private final PasswordSmsService passwordSmsService;

    /**
     * Handles password reset SMS request events.
     *
     * <p>Sends the pre-generated verification code to the user's verified
     * phone number. The code is already generated and stored in JWT token,
     * so we must use the code from the event to maintain consistency.
     *
     * @param event the password reset SMS request event
     */
    @EventListener
    @Async("smsExecutor")
    public void handlePasswordResetSmsRequested(PasswordResetSmsRequestedEvent event) {
        log.debug("Handling password reset SMS request for user: {}", event.user().getUsername());

        try {
            passwordSmsService.sendPasswordResetCode(
                    event.user(),
                    event.verificationCode()
            );

            log.debug("Password reset SMS triggered successfully for user: {}", event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password reset SMS for user: {} - error: {}",
                    event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - SMS failure should not affect password reset process
        }
    }

    /**
     * Handles password change SMS alert events.
     * 
     * <p>Sends a security alert SMS to notify the user that their password
     * was changed. This helps detect unauthorized password changes.
     * 
     * @param event the password change SMS alert event
     */
    @EventListener
    @Async("smsExecutor")
    public void handlePasswordChangeSmsAlert(PasswordChangeSmsAlertEvent event) {
        log.debug("Handling password change SMS alert for user: {}", event.user().getUsername());
        
        try {
            passwordSmsService.sendPasswordChangeAlert(event.user());
            log.debug("Password change SMS alert triggered successfully for user: {}", 
                     event.user().getUsername());
        } catch (Exception e) {
            log.error("Failed to send password change SMS alert for user: {} - error: {}", 
                     event.user().getUsername(), e.getMessage(), e);
            // Don't rethrow - SMS failure should not affect password change process
        }
    }
}