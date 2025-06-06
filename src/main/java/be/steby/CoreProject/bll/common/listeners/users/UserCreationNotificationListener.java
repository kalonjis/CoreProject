package be.steby.CoreProject.bll.common.listeners.users;


import be.steby.CoreProject.bll.common.event.user.AdminUserCreatedEvent;
import be.steby.CoreProject.bll.common.event.user.SelfSignupUserCreatedEvent;
import be.steby.CoreProject.bll.common.event.user.SystemUserCreatedEvent;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(20) // after activity logs
@Slf4j
public class UserCreationNotificationListener {

    private final MailerService mailerService;

    /**
     * Manages notification for self-registration
     */
    @EventListener
    @Async("emailExecutor")
    public void handleSelfSignupUserCreated(SelfSignupUserCreatedEvent event) {
        log.info("Send confirmation email for self-registration: {}",
                event.user().getUsername());

        try {
            mailerService.sendSignUpConfirmation(
                    event.confirmationToken(),
                    event.user()
            );
            log.info("Confirmation email successfully sent for {}",
                    event.user().getUsername());
        } catch (Exception e) {
            log.error("Error sending confirmation email for {}: {}",
                    event.user().getUsername(), e.getMessage(), e);
        }
    }


    /**
     * Manages notification of creation by admin
     */
    @EventListener
    @Async("emailExecutor")
    public void handleAdminUserCreated(AdminUserCreatedEvent event) {
        log.info("Send confirmation email of creation by admin for: {}",
                event.user().getUsername());

        try {
            mailerService.sendAccountConfirmation(
                    event.confirmationToken(),
                    event.user(),
                    event.temporaryPassword()
            );
            log.info("Admin creation email successfully sent to {}",
                    event.user().getUsername());
        } catch (Exception e) {
            log.error("Error sending admin creation email for {}: {}",
                    event.user().getUsername(), e.getMessage(), e);
        }
    }

    /**
     * Gère la notification pour la création système (si nécessaire)
     */
    @EventListener
    @Async("emailExecutor")
    public void handleSystemUserCreated(SystemUserCreatedEvent event) {
        log.info("Utilisateur système créé: {} (pas d'email envoyé)",
                event.user().getUsername());

        // Pour les utilisateurs système, on peut ne pas envoyer d'email
        // ou envoyer un email différent selon les besoins
    }




}
