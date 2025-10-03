package be.steby.CoreProject.bll.domains.admin.listeners;

import be.steby.CoreProject.bll.domains.account.services.AccountMailerService;
import be.steby.CoreProject.bll.domains.admin.events.AdminUserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener for AdminUserCreatedEvent.
 * Handles sending email with temporary password to admin-created users.
 * No confirmation link needed - user confirms by first login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserCreationEmailListener {

    private final AccountMailerService accountMailerService;

    /**
     * Handles AdminUserCreatedEvent by sending welcome email with temporary password.
     * User confirms account by logging in with the temporary password.
     *
     * @param event The admin user created event
     */
    @EventListener
    public void handleAdminUserCreated(AdminUserCreatedEvent event) {
        log.info("Handling AdminUserCreatedEvent for user: {} (created by: {})",
                event.getCreatedUsername(), event.getAdminUsername());

        try {
            // Send email with temporary password (no confirmation link needed)
            accountMailerService.sendAdminCreatedUserEmail(
                    event.createdUser(),
                    event.temporaryPassword()
            );

            log.info("Admin creation email sent successfully to: {}", event.getCreatedUserEmail());

        } catch (Exception e) {
            log.error("Failed to send admin creation email to: {}. Error: {}",
                    event.getCreatedUserEmail(), e.getMessage(), e);
        }
    }
}