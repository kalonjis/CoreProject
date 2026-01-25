package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorInitiateActivationEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorSmsActivationEvent;
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
 * NOTE: Login 2FA verification codes are now sent SYNCHRONOUSLY directly
 * from AuthService, not via events. This listener only handles:
 * - 2FA activation codes (during setup)
 * - 2FA enabled confirmations
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
     *
     * Sends a confirmation email/SMS when 2FA is successfully enabled.
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
     * Handles TwoFactorSmsActivationEvent by sending verification code via SMS.
     *
     * This method processes TwoFactorSmsActivationEvent by sending the
     * verification code via SMS to the user's phone number. This is specifically
     * for the activation/setup phase of SMS 2FA.
     *
     * The SMS message contains the 6-digit verification code that the user
     * must enter to complete the SMS 2FA activation process.
     *
     * Runs asynchronously to avoid blocking the main activation flow.
     * SMS delivery failures are logged but do not affect the activation process.
     *
     * @param event the SMS two-factor activation event containing user, code, and phone number
     */
    @EventListener
    @Async("smsExecutor")
    public void handleTwoFactorSmsActivationInitiated(TwoFactorSmsActivationEvent event) {
        log.debug("Handling SMS 2FA activation for user: {} to phone: {}",
                event.user().getUsername(),
                maskPhoneNumber(event.phoneNumber()));

        try {
            // Send activation verification code via SMS
            authSmsService.sendTwoFactorActivationCode(
                    event.user(),
                    event.verificationCode(),
                    event.phoneNumber()
            );

            log.info("SMS 2FA activation code sent successfully to user: {} (phone: {})",
                    event.user().getUsername(),
                    maskPhoneNumber(event.phoneNumber()));

        } catch (Exception e) {
            // Log error but don't fail the activation initiation
            // The user can request a resend if SMS delivery fails
            log.error("Failed to send SMS 2FA activation code to user: {} (phone: {}) - Error: {}",
                    event.user().getUsername(),
                    maskPhoneNumber(event.phoneNumber()),
                    e.getMessage(), e);
        }
    }

    /**
     * Masks a phone number for logging purposes.
     * Shows only last 4 digits for privacy.
     *
     * @param phoneNumber The phone number to mask
     * @return Masked phone number (e.g., "****1234")
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}