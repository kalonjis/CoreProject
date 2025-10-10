package be.steby.CoreProject.bll.domains.admin.listeners;

import be.steby.CoreProject.bll.domains.admin.events.password.AdminPasswordResetLinkEvent;
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
}