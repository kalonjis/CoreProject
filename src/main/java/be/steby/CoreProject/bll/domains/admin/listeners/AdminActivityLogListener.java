package be.steby.CoreProject.bll.domains.admin.listeners;

import be.steby.CoreProject.bll.domains.admin.events.*;
import be.steby.CoreProject.bll.domains.admin.services.AdminActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener pour enregistrer les événements d'administration dans les logs d'activité.
 * Utilise un executeur asynchrone dédié pour ne pas bloquer le thread principal.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminActivityLogListener {

    private final AdminActivityLogService adminActivityLogService;

    /**
     * Écoute les événements de création d'utilisateur par un administrateur.
     *
     * @param event Événement de création d'utilisateur par admin
     */
    @EventListener
    @Async("auditTaskExecutor")
    @Order(100) // S'exécute après les listeners principaux
    public void handleUserCreatedByAdmin(UserCreatedByAdminEvent event) {
        try {
            log.debug("Traitement de l'événement de création d'utilisateur par admin: {}", event.getDescription());

            adminActivityLogService.logUserCreation(
                    event.adminUser(),
                    event.createdUser(),
                    event.assignedRoles(),
                    event.autoActivated(),
                    event.requestContext()
            );

            log.debug("Événement de création d'utilisateur par admin enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'événement de création d'utilisateur par admin", e);
        }
    }

    /**
     * Écoute les événements d'attribution de rôle.
     *
     * @param event Événement d'attribution de rôle
     */
    @EventListener
    @Async("auditTaskExecutor")
    @Order(100)
    public void handleUserRoleGranted(UserRoleGrantedEvent event) {
        try {
            log.debug("Traitement de l'événement d'attribution de rôle: {}", event.getDescription());

            adminActivityLogService.logRoleGrant(
                    event.adminUser(),
                    event.targetUser(),
                    event.grantedRole(),
                    event.previousRoles(),
                    event.currentRoles(),
                    event.reason(),
                    event.requestContext()
            );

            log.debug("Événement d'attribution de rôle enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'événement d'attribution de rôle", e);
        }
    }

    /**
     * Écoute les événements de révocation de rôle.
     *
     * @param event Événement de révocation de rôle
     */
    @EventListener
    @Async("auditTaskExecutor")
    @Order(100)
    public void handleUserRoleRevoked(UserRoleRevokedEvent event) {
        try {
            log.debug("Traitement de l'événement de révocation de rôle: {}", event.getDescription());

            adminActivityLogService.logRoleRevoke(
                    event.adminUser(),
                    event.targetUser(),
                    event.revokedRole(),
                    event.previousRoles(),
                    event.currentRoles(),
                    event.reason(),
                    event.requestContext()
            );

            log.debug("Événement de révocation de rôle enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'événement de révocation de rôle", e);
        }
    }

    /**
     * Écoute les événements d'activation d'utilisateur par un admin.
     *
     * @param event Événement d'activation d'utilisateur par admin
     */
    @EventListener
    @Async("auditTaskExecutor")
    @Order(100)
    public void handleAdminUserActivated(AdminUserActivatedEvent event) {
        try {
            log.debug("Traitement de l'événement d'activation d'utilisateur par admin: {}", event.getDescription());

            adminActivityLogService.logUserActivation(
                    event.adminUser(),
                    event.targetUser(),
                    event.wasPreviouslyDeactivated(),
                    event.requestContext()
            );

            log.debug("Événement d'activation d'utilisateur par admin enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'événement d'activation d'utilisateur par admin", e);
        }
    }

    /**
     * Écoute les événements de désactivation d'utilisateur par un admin.
     *
     * @param event Événement de désactivation d'utilisateur par admin
     */
    @EventListener
    @Async("auditTaskExecutor")
    @Order(100)
    public void handleAdminUserDeactivated(AdminUserDeactivatedEvent event) {
        try {
            log.debug("Traitement de l'événement de désactivation d'utilisateur par admin: {}", event.getDescription());

            adminActivityLogService.logUserDeactivation(
                    event.adminUser(),
                    event.targetUser(),
                    event.category(),
                    event.comment(),
                    event.invalidateActiveSessions(),
                    event.requestContext()
            );

            // Log supplémentaire pour les désactivations sensibles
            if (event.isSecurityRelated()) {
                log.warn("Désactivation d'utilisateur pour raisons de sécurité: {} par {}",
                        event.getTargetUsername(), event.getAdminUsername());
            }

            log.debug("Événement de désactivation d'utilisateur par admin enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'événement de désactivation d'utilisateur par admin", e);
        }
    }

    /**
     * Écoute les événements de reset de mot de passe déclenché par un admin.
     *
     * @param event Événement de reset de mot de passe par admin
     */
    @EventListener
    @Async("auditTaskExecutor")
    @Order(100)
    public void handleAdminPasswordResetTriggered(AdminPasswordResetTriggeredEvent event) {
        try {
            log.debug("Traitement de l'événement de reset de mot de passe par admin: {}", event.getDescription());

            adminActivityLogService.logPasswordResetTriggered(
                    event.adminUser(),
                    event.targetUser(),
                    event.reason(),
                    event.forceChangeOnNextLogin(),
                    event.invalidateActiveSessions(),
                    event.requestContext()
            );

            // Log supplémentaire pour les resets urgents
            if (event.isUrgentReset()) {
                log.warn("Reset de mot de passe urgent: {} pour {} par {}",
                        event.getPriorityLevel(), event.getTargetUsername(), event.getAdminUsername());
            }

            log.debug("Événement de reset de mot de passe par admin enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'événement de reset de mot de passe par admin", e);
        }
    }

    /**
     * Écoute les événements d'action administrative générique.
     *
     * @param event Événement d'action administrative
     */
    @EventListener
    @Async("auditTaskExecutor")
    @Order(100)
    public void handleAdminAction(AdminActionEvent event) {
        try {
            log.debug("Traitement de l'événement d'action administrative: {}", event.getFullDescription());

            adminActivityLogService.logAdminAction(event);

            // Log supplémentaire pour les actions de sécurité
            if (event.isSecurityAction()) {
                log.info("Action de sécurité administrative: {} par {}",
                        event.actionDescription(), event.getAdminUsername());
            }

            // Log d'alerte pour les échecs d'actions critiques
            if (event.isFailure() && event.isSecurityAction()) {
                log.warn("Échec d'action de sécurité administrative: {} par {} - Erreur: {}",
                        event.actionDescription(), event.getAdminUsername(), event.resultMessage());
            }

            log.debug("Événement d'action administrative enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'événement d'action administrative", e);
        }
    }

    /**
     * Méthode utilitaire pour loguer les détails d'un événement.
     *
     * @param eventType Type d'événement
     * @param adminUsername Nom de l'administrateur
     * @param targetUsername Nom de l'utilisateur cible (optionnel)
     * @param details Détails additionnels
     */
    private void logEventDetails(String eventType, String adminUsername, String targetUsername, String details) {
        if (targetUsername != null) {
            log.info("Événement {}: Admin '{}' -> Utilisateur '{}' | {}",
                    eventType, adminUsername, targetUsername, details);
        } else {
            log.info("Événement {}: Admin '{}' | {}",
                    eventType, adminUsername, details);
        }
    }

    /**
     * Méthode utilitaire pour loguer les erreurs d'événements.
     *
     * @param eventType Type d'événement
     * @param adminUsername Nom de l'administrateur
     * @param error Erreur rencontrée
     */
    private void logEventError(String eventType, String adminUsername, Throwable error) {
        log.error("Erreur lors du traitement de l'événement {} pour l'admin '{}': {}",
                eventType, adminUsername, error.getMessage(), error);
    }
}