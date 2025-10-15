package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
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
    
    private final MailerService mailerService;
    
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
            // TODO: Create specific method in MailerService for 2FA confirmation
            // For now, we'll use a generic approach
            mailerService.sendTwoFactorEnabledConfirmation(event.user(), event.type());
            
            log.info("2FA enabled confirmation email sent to user: {}", event.user().getEmail());
            
        } catch (Exception e) {
            // Log error but don't fail the 2FA enable operation
            log.error("Failed to send 2FA confirmation email to user: {}", 
                      event.user().getEmail(), e);
        }
    }
}