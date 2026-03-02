package be.steby.CoreProject.bll.domains.admin.listeners;

import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.password.AdminAlternativeChannelPasswordEvent;
import be.steby.CoreProject.bll.domains.admin.events.password.AdminPasswordResetLinkEvent;
import be.steby.CoreProject.bll.domains.admin.events.password.AdminTemporaryPasswordSentEvent;
import be.steby.CoreProject.bll.domains.admin.services.AdminMailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserAccountNotificationListener {

    private final AdminMailerService adminMailerService;


    /**
     * Handles {@link AdminUserDeactivatedEvent} to notify the target user
     * that their account has been administratively deactivated.
     *
     * Runs asynchronously on the email executor — a delivery failure never
     * rolls back the deactivation itself.
     *
     * @param event the admin user deactivated event
     */
    @EventListener
    @Async("emailExecutor")
    public void handleAdminUserDeactivated(AdminUserDeactivatedEvent event) {
        log.info("Sending admin deactivation email to: {} — category: {}",
                event.targetUser().getEmail(), event.category());
        try {
            adminMailerService.sendAdminDeactivationEmail(event);
            log.info("Admin deactivation email sent to: {}", event.targetUser().getEmail());
        } catch (Exception e) {
            log.error("Failed to send admin deactivation email to: {} — {}",
                    event.targetUser().getEmail(), e.getMessage(), e);
        }
    }

}