package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorVerificationRequestedEvent;
import be.steby.CoreProject.bll.domains.auth.services.mailer.AuthMailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for two-factor authentication events.
 * Handles email notifications for 2FA operations.
 * 
 * Uses async email executor to avoid blocking the main thread.
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
    
    /**
     * Handles TwoFactorEnabledEvent by sending confirmation email.
     * Runs asynchronously to not block the main authentication flow.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleTwoFactorEnabled(TwoFactorEnabledEvent event) {
        log.debug("Handling TwoFactorEnabledEvent for user: {} with type: {}", 
                  event.user().getEmail(), event.type());
        
        try {

            authMailerService.sendTwoFactorEnabledConfirmation(event.user(), event.type());
            
            log.info("2FA enabled confirmation email sent to user: {}", event.user().getEmail());
            
        } catch (Exception e) {
            // Log error but don't fail the 2FA enable operation
            log.error("Failed to send 2FA confirmation email to user: {}", 
                      event.user().getEmail(), e);
        }
    }


    /**
     * Handles 2FA verification code requests by sending email with verification code.
     * Triggered during login initiation when 2FA is required and during code resends.
     *
     * @param event Event containing user info and verification code
     */
    @EventListener
    @Async("emailExecutor")
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
                case TOTP -> {
                    // Future implementation for TOTP apps - no email needed
                    log.debug("TOTP verification - no email needed");
                }
                case SMS -> {
                    // Future implementation for SMS - not yet implemented
                    log.debug("SMS verification requested - not yet implemented");
                }
                default -> {
                    log.warn("Unknown 2FA type: {}", event.twoFactorType());
                }
            }

        } catch (Exception e) {
            // Log error but don't fail the authentication flow
            log.error("Failed to send 2FA verification code to user: {} - Error: {}",
                    event.user().getEmail(), e.getMessage(), e);
        }
    }
}
