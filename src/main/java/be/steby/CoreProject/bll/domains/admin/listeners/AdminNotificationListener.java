package be.steby.CoreProject.bll.domains.admin.listeners;

import be.steby.CoreProject.bll.domains.admin.events.password.AdminAlternativeChannelPasswordEvent;
import be.steby.CoreProject.bll.domains.admin.events.password.AdminPasswordResetLinkEvent;
import be.steby.CoreProject.bll.domains.admin.events.password.AdminTemporaryPasswordSentEvent;
import be.steby.CoreProject.bll.domains.admin.services.AdminMailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for admin-related events that handles email notifications.
 * Listens to password reset link events initiated by administrators.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationListener {

    private final AdminMailerService adminMailerService;

    /**
     * Handles AdminPasswordResetLinkEvent by sending password reset link email.
     *
     * @param event The admin password reset link event
     */
    @EventListener
    @Async("emailExecutor")
    public void handlePasswordResetLink(AdminPasswordResetLinkEvent event) {
        log.info("Handling AdminPasswordResetLinkEvent for user: {} (initiated by: {})",
                event.target().getEmail(), event.admin().getUsername());

        try {
            adminMailerService.sendPasswordResetLink(
                    event.tokenPublicId(),
                    event.target(),
                    event.admin(),
                    event.reason()
            );

            log.info("Password reset link email sent successfully to: {}", event.target().getEmail());

        } catch (Exception e) {
            log.error("Failed to send password reset link email to: {}. Error: {}",
                    event.target().getEmail(), e.getMessage(), e);
        }
    }


    /**
     * Handles AdminTemporaryPasswordSentEvent by sending temporary password email.
     *
     * @param event The admin temporary password sent event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleTemporaryPasswordSent(AdminTemporaryPasswordSentEvent event) {
        log.info("Handling AdminTemporaryPasswordSentEvent for user: {} (initiated by: {})",
                event.target().getEmail(), event.admin().getUsername());

        try {
            adminMailerService.sendTemporaryPassword(
                    event.temporaryPassword(),
                    event.target(),
                    event.admin(),
                    event.reason()
            );

            log.info("Temporary password email sent successfully to: {}", event.target().getEmail());

        } catch (Exception e) {
            log.error("Failed to send temporary password email to: {}. Error: {}",
                    event.target().getEmail(), e.getMessage(), e);
        }
    }

    // Dans AdminNotificationListener.java

    /**
     * Handles AdminAlternativeChannelPasswordEvent by sending temporary password
     * via alternative channels (email and/or SMS).
     *
     * @param event The admin alternative channel password event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleAlternativeChannelPassword(AdminAlternativeChannelPasswordEvent event) {
        log.warn("⚠️ Handling AdminAlternativeChannelPasswordEvent for user: {} via {} (initiated by: {})",
                event.target().getEmail(),
                event.deliveryMethod(),
                event.admin().getUsername());

        boolean shouldSendEmail = event.deliveryMethod().contains("EMAIL") &&
                event.alternativeEmail() != null && !event.alternativeEmail().isBlank();

        boolean shouldSendSMS = event.deliveryMethod().contains("SMS") &&
                event.alternativePhone() != null && !event.alternativePhone().isBlank();


        try {
            // Send via email if provided
            if (shouldSendEmail) {
                adminMailerService.sendTemporaryPasswordToAlternativeEmail(
                        event.temporaryPassword(),
                        event.target(),
                        event.admin(),
                        event.reason(),
                        event.alternativeEmail()
                );
                log.info("✅ Temporary password sent to alternative email: {}", event.alternativeEmail());
            }

            // Send via SMS if provided
            if (shouldSendSMS) {
                // TODO: Implement SMS service
                log.warn("📱 SMS delivery not yet implemented - would send to: {}", event.alternativePhone());
                // smsService.sendTemporaryPassword(event.alternativePhone(), event.temporaryPassword());
            }

            log.info("✅ Alternative channel password delivery completed for: {}", event.target().getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send temporary password via alternative channel to: {}. Error: {}",
                    event.target().getEmail(), e.getMessage(), e);
        }
    }
}