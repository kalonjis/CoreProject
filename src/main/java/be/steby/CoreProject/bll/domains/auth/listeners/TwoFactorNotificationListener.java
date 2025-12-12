package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorInitiateActivationEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorVerificationRequestedEvent;
import be.steby.CoreProject.bll.domains.auth.services.notifications.email.AuthMailerService;
import be.steby.CoreProject.bll.domains.auth.services.notifications.sms.AuthSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for two-factor authentication events.
 * Handles email and SMS notifications for 2FA operations.
 *
 * Uses async executors to avoid blocking the main thread:
 * - emailExecutor for email notifications
 * - smsExecutor for SMS notifications
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class TwoFactorNotificationListener {

    private final AuthMailerService authMailerService;
    private final AuthSmsService authSmsService;

    /**
     * Handles two-factor authentication activation initiation events.
     *
     * This method processes TwoFactorInitiateActivationEvent by sending the
     * verification code via email to the user. This is specifically for the
     * activation/setup phase, different from login verification codes.
     *
     * The email template used should be specific to the activation process
     * and may include additional instructions about completing the 2FA setup.
     *
     * Runs asynchronously to avoid blocking the main activation flow.
     * Email delivery failures are logged but do not affect the activation process.
     *
     * @param event the two-factor activation initiation event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleTwoFactorActivationInitiated(TwoFactorInitiateActivationEvent event) {
        log.debug("Handling 2FA activation initiation for user: {}", event.user().getEmail());

        try {
            // Send activation verification code via email
            authMailerService.sendTwoFactorActivationCode(
                    event.user(),
                    event.verificationCode()
            );

            log.info("2FA activation verification email sent successfully to user: {}",
                    event.user().getEmail());

        } catch (Exception e) {
            // Log error but don't fail the activation initiation
            log.error("Failed to send 2FA activation verification email to user: {} - Error: {}",
                    event.user().getUsername(), e.getMessage(), e);
        }
    }


    /**
     * Handles TwoFactorEnabledEvent by sending confirmation via appropriate channel.
     * Runs asynchronously to not block the main authentication flow.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleTwoFactorEnabled(TwoFactorEnabledEvent event) {
        log.debug("Handling TwoFactorEnabledEvent for user: {} with type: {}",
                event.user().getEmail(), event.type());

        try {
            switch (event.type()) {
                case EMAIL, TOTP, BACKUP_CODES -> {
                    // Email notification for these types
                    authMailerService.sendTwoFactorEnabledConfirmation(event.user(), event.type());
                    log.info("2FA enabled confirmation email sent to user: {}", event.user().getEmail());
                }

                default -> {
                    log.warn("Unknown 2FA type for enabled confirmation: {}", event.type());
                }
            }

        } catch (Exception e) {
            // Log error but don't fail the 2FA enable operation
            log.error("Failed to send 2FA enabled confirmation to user: {} for type: {} - Error: {}",
                    event.user().getUsername(), event.type(), e.getMessage(), e);
        }
    }

    /**
     * Handles 2FA verification code requests by sending code via appropriate channel.
     * Triggered during login initiation when 2FA is required and during code resends.
     *
     * @param event Event containing user info and verification code
     */
    @EventListener
    @Async("emailExecutor") // Default executor, but SMS will use smsExecutor internally
    public void handleTwoFactorVerificationRequested(TwoFactorVerificationRequestedEvent event) {
        log.debug("Handling 2FA verification request for user: {} with type: {}",
                event.user().getEmail(), event.twoFactorType());

        try {
            switch (event.twoFactorType()) {
                case EMAIL -> {
                    authMailerService.sendTwoFactorVerificationCode(
                            event.user(),
                            event.verificationCode(),
                            event.httpRequest()
                    );
                    log.info("2FA verification email sent to user: {}", event.user().getEmail());
                }
                case SMS -> {
                    authSmsService.sendTwoFactorVerificationCode(
                            event.user(),
                            event.verificationCode()
                    );
                    log.info("2FA verification SMS sent to user: {}", event.user().getUsername());
                }
                case TOTP -> {
                    // TOTP apps don't need external notification - code is generated in app
                    log.debug("TOTP verification - no external notification needed");
                }
                case BACKUP_CODES -> {
                    // Backup codes don't need external notification - user has them saved
                    log.debug("Backup codes verification - no external notification needed");
                }
                default -> {
                    log.warn("Unknown 2FA type for verification: {}", event.twoFactorType());
                }
            }

        } catch (Exception e) {
            // Log error but don't fail the authentication flow
            log.error("Failed to send 2FA verification code to user: {} for type: {} - Error: {}",
                    event.user().getUsername(), event.twoFactorType(), e.getMessage(), e);
        }
    }
}