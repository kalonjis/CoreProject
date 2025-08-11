//package be.steby.CoreProject.bll.domains.admin.listeners;
//
//import be.steby.CoreProject.bll.domains.admin.events.*;
//import be.steby.CoreProject.bll.common.services.mailer.MailerService;
//import be.steby.CoreProject.bll.domains.user.services.UserService;
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.enums.UserRole;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.EventListener;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Listener pour gérer les notifications suite aux actions d'administration.
// * Ce listener envoie des emails aux utilisateurs concernés
// * et aux super-administrateurs pour les actions sensibles.
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class AdminNotificationListener {
//
//    private final MailerService mailerService;
//    private final UserService userService;
//
//    /**
//     * Gère les notifications pour la création d'utilisateur par un admin.
//     *
//     * @param event Événement de création d'utilisateur par admin
//     */
//    @EventListener
//    @Async("notificationTaskExecutor")
//    @Order(200) // S'exécute après les listeners d'audit
//    public void handleUserCreatedByAdmin(UserCreatedByAdminEvent event) {
//        try {
//            log.debug("Traitement des notifications pour création d'utilisateur par admin: {}", event.getCreatedUsername());
//
//            // Notification à l'utilisateur créé
//            sendUserCreationNotification(event);
//
//            // Notification aux super-admins si des rôles administratifs ont été attribués
//            if (event.hasAdministrativeRoles()) {
//                sendSuperAdminAlertForUserCreation(event);
//            }
//
//            log.debug("Notifications de création d'utilisateur envoyées avec succès");
//        } catch (Exception e) {
//            log.error("Erreur lors de l'envoi des notifications de création d'utilisateur", e);
//        }
//    }
//
//    /**
//     * Gère les notifications pour l'attribution de rôle.
//     *
//     * @param event Événement d'attribution de rôle
//     */
//    @EventListener
//    @Async("notificationTaskExecutor")
//    @Order(200)
//    public void handleUserRoleGranted(UserRoleGrantedEvent event) {
//        try {
//            log.debug("Traitement des notifications pour attribution de rôle: {}", event.getDescription());
//
//            // Notification à l'utilisateur concerné
//            sendRoleGrantNotification(event);
//
//            // Notification spéciale pour les rôles administratifs
//            if (event.isAdministrativeRole()) {
//                sendAdministrativeRoleGrantedNotification(event);
//            }
//
//            // Alerte super-admin pour attribution SUPER_ADMIN
//            if (event.isSuperAdminRole()) {
//                sendSuperAdminRoleGrantedAlert(event);
//            }
//
//            log.debug("Notifications d'attribution de rôle envoyées avec succès");
//        } catch (Exception e) {
//            log.error("Erreur lors de l'envoi des notifications d'attribution de rôle", e);
//        }
//    }
//
//    /**
//     * Gère les notifications pour la révocation de rôle.
//     *
//     * @param event Événement de révocation de rôle
//     */
//    @EventListener
//    @Async("notificationTaskExecutor")
//    @Order(200)
//    public void handleUserRoleRevoked(UserRoleRevokedEvent event) {
//        try {
//            log.debug("Traitement des notifications pour révocation de rôle: {}", event.getDescription());
//
//            // Notification à l'utilisateur concerné
//            sendRoleRevokeNotification(event);
//
//            // Notification spéciale si l'utilisateur perd tous ses privilèges administratifs
//            if (event.losesAllAdministrativeRoles()) {
//                sendAdministrativePrivilegesLostNotification(event);
//            }
//
//            // Alerte super-admin pour révocation SUPER_ADMIN
//            if (event.isSuperAdminRole()) {
//                sendSuperAdminRoleRevokedAlert(event);
//            }
//
//            log.debug("Notifications de révocation de rôle envoyées avec succès");
//        } catch (Exception e) {
//            log.error("Erreur lors de l'envoi des notifications de révocation de rôle", e);
//        }
//    }
//
//    /**
//     * Gère les notifications pour l'activation d'utilisateur.
//     *
//     * @param event Événement d'activation d'utilisateur par admin
//     */
//    @EventListener
//    @Async("notificationTaskExecutor")
//    @Order(200)
//    public void handleAdminUserActivated(AdminUserActivatedEvent event) {
//        try {
//            log.debug("Traitement des notifications pour activation d'utilisateur: {}", event.getTargetUsername());
//
//            // Notification à l'utilisateur activé
//            sendUserActivationNotification(event);
//
//            // Notification spéciale pour la réactivation
//            if (event.isReactivation()) {
//                sendUserReactivationNotification(event);
//            }
//
//            log.debug("Notifications d'activation d'utilisateur envoyées avec succès");
//        } catch (Exception e) {
//            log.error("Erreur lors de l'envoi des notifications d'activation d'utilisateur", e);
//        }
//    }
//
//    /**
//     * Gère les notifications pour la désactivation d'utilisateur.
//     *
//     * @param event Événement de désactivation d'utilisateur par admin
//     */
//    @EventListener
//    @Async("notificationTaskExecutor")
//    @Order(200)
//    public void handleAdminUserDeactivated(AdminUserDeactivatedEvent event) {
//        try {
//            log.debug("Traitement des notifications pour désactivation d'utilisateur: {}", event.getTargetUsername());
//
//            // Notification à l'utilisateur désactivé
//            sendUserDeactivationNotification(event);
//
//            // Alerte super-admin pour désactivations sensibles
//            if (event.isSecurityRelated() || event.isAdministrativeUser()) {
//                sendSuperAdminDeactivationAlert(event);
//            }
//
//            log.debug("Notifications de désactivation d'utilisateur envoyées avec succès");
//        } catch (Exception e) {
//            log.error("Erreur lors de l'envoi des notifications de désactivation d'utilisateur", e);
//        }
//    }
//
//    /**
//     * Gère les notifications pour le reset de mot de passe.
//     *
//     * @param event Événement de reset de mot de passe par admin
//     */
//    @EventListener
//    @Async("notificationTaskExecutor")
//    @Order(200)
//    public void handleAdminPasswordResetTriggered(AdminPasswordResetTriggeredEvent event) {
//        try {
//            log.debug("Traitement des notifications pour reset de mot de passe: {}", event.getTargetUsername());
//
//            // Notification à l'utilisateur concerné
//            sendPasswordResetNotification(event);
//
//            // Alerte super-admin pour resets urgents ou utilisateurs administratifs
//            if (event.isUrgentReset() || event.isAdministrativeUser()) {
//                sendSuperAdminPasswordResetAlert(event);
//            }
//
//            log.debug("Notifications de reset de mot de passe envoyées avec succès");
//        } catch (Exception e) {
//            log.error("Erreur lors de l'envoi des notifications de reset de mot de passe", e);
//        }
//    }
//
//    /**
//     * Gère les notifications pour les actions administratives génériques.
//     *
//     * @param event Événement d'action administrative
//     */
//    @EventListener
//    @Async("notificationTaskExecutor")
//    @Order(200)
//    public void handleAdminAction(AdminActionEvent event) {
//        try {
//            log.debug("Traitement des notifications pour action administrative: {}", event.actionType());
//
//            // Notifications uniquement pour les actions de sécurité critiques
//            if (event.isSecurityAction() && event.isFailure()) {
//                sendSecurityActionFailureAlert(event);
//            }
//
//            log.debug("Notifications d'action administrative traitées avec succès");
//        } catch (Exception e) {
//            log.error("Erreur lors de l'envoi des notifications d'action administrative", e);
//        }
//    }
//
//    // ================== MÉTHODES PRIVÉES DE NOTIFICATION ==================
//
//    /**
//     * Envoie une notification de création d'utilisateur.
//     */
//    private void sendUserCreationNotification(UserCreatedByAdminEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getCreatedUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("assignedRoles", event.assignedRoles());
//        templateData.put("autoActivated", event.autoActivated());
//
//        mailerService.sendTemplateEmail(
//                event.createdUser().getEmail(),
//                "Création de votre compte par un administrateur",
//                "admin/user-created-by-admin",
//                templateData
//        );
//    }
//
//    /**
//     * Envoie une notification d'attribution de rôle.
//     */
//    private void sendRoleGrantNotification(UserRoleGrantedEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("grantedRole", event.grantedRole());
//        templateData.put("category", event.category());
//        templateData.put("reason", event.category().getDisplayName());
//        templateData.put("isAdministrativeRole", event.isAdministrativeRole());
//
//        mailerService.sendTemplateEmail(
//                event.targetUser().getEmail(),
//                "Nouveau rôle attribué à votre compte",
//                "admin/role-granted",
//                templateData
//        );
//    }
//
//    /**
//     * Envoie une notification de révocation de rôle.
//     */
//    private void sendRoleRevokeNotification(UserRoleRevokedEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("revokedRole", event.revokedRole());
//        templateData.put("reason", event.reason());
//        templateData.put("currentRoles", event.currentRoles());
//
//        mailerService.sendTemplateEmail(
//                event.targetUser().getEmail(),
//                "Rôle révoqué de votre compte",
//                "admin/role-revoked",
//                templateData
//        );
//    }
//
//    /**
//     * Envoie une notification d'activation d'utilisateur.
//     */
//    private void sendUserActivationNotification(AdminUserActivatedEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("isReactivation", event.isReactivation());
//
//        String subject = event.isReactivation() ? "Votre compte a été réactivé" : "Votre compte a été activé";
//        String template = event.isReactivation() ? "admin/user-reactivated" : "admin/user-activated";
//
//        mailerService.sendTemplateEmail(
//                event.targetUser().getEmail(),
//                subject,
//                template,
//                templateData
//        );
//    }
//
//    /**
//     * Envoie une notification de désactivation d'utilisateur.
//     */
//    private void sendUserDeactivationNotification(AdminUserDeactivatedEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("reason", event.reason());
//        templateData.put("comment", event.comment());
//        templateData.put("isTemporary", event.isTemporary());
//
//        mailerService.sendTemplateEmail(
//                event.targetUser().getEmail(),
//                "Votre compte a été désactivé",
//                "admin/user-deactivated",
//                templateData
//        );
//    }
//
//    /**
//     * Envoie une notification de reset de mot de passe.
//     */
//    private void sendPasswordResetNotification(AdminPasswordResetTriggeredEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("reason", event.reason());
//        templateData.put("forceChangeOnNextLogin", event.forceChangeOnNextLogin());
//        templateData.put("isSecurityRelated", event.isSecurityRelated());
//
//        mailerService.sendTemplateEmail(
//                event.targetUser().getEmail(),
//                "Reset de votre mot de passe par un administrateur",
//                "admin/password-reset-by-admin",
//                templateData
//        );
//    }
//
//    // ================== ALERTES SUPER-ADMIN ==================
//
//    /**
//     * Envoie une alerte aux super-admins pour la création d'utilisateur avec rôles administratifs.
//     */
//    private void sendSuperAdminAlertForUserCreation(UserCreatedByAdminEvent event) {
//        List<User> superAdmins = userService.getUsersByRole(UserRole.SUPER_ADMIN);
//
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("createdUsername", event.getCreatedUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("assignedRoles", event.assignedRoles());
//        templateData.put("hasSuperAdminRole", event.hasSuperAdminRole());
//
//        for (User superAdmin : superAdmins) {
//            if (!superAdmin.getId().equals(event.getAdminUserId())) { // Ne pas notifier l'admin qui a fait l'action
//                mailerService.sendTemplateEmail(
//                        superAdmin.getEmail(),
//                        "[ALERTE] Création d'utilisateur avec rôles administratifs",
//                        "admin/alerts/user-created-with-admin-roles",
//                        templateData
//                );
//            }
//        }
//    }
//
//    /**
//     * Envoie une alerte pour l'attribution du rôle SUPER_ADMIN.
//     */
//    private void sendSuperAdminRoleGrantedAlert(UserRoleGrantedEvent event) {
//        List<User> superAdmins = userService.getUsersByRole(UserRole.SUPER_ADMIN);
//
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("targetUsername", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("reason", event.reason());
//
//        for (User superAdmin : superAdmins) {
//            if (!superAdmin.getId().equals(event.getAdminUserId()) &&
//                    !superAdmin.getId().equals(event.getTargetUserId())) {
//                mailerService.sendTemplateEmail(
//                        superAdmin.getEmail(),
//                        "[ALERTE CRITIQUE] Attribution du rôle SUPER_ADMIN",
//                        "admin/alerts/super-admin-role-granted",
//                        templateData
//                );
//            }
//        }
//    }
//
//    /**
//     * Envoie une alerte pour la révocation du rôle SUPER_ADMIN.
//     */
//    private void sendSuperAdminRoleRevokedAlert(UserRoleRevokedEvent event) {
//        List<User> superAdmins = userService.getUsersByRole(UserRole.SUPER_ADMIN);
//
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("targetUsername", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("reason", event.reason());
//
//        for (User superAdmin : superAdmins) {
//            if (!superAdmin.getId().equals(event.getAdminUserId())) {
//                mailerService.sendTemplateEmail(
//                        superAdmin.getEmail(),
//                        "[ALERTE CRITIQUE] Révocation du rôle SUPER_ADMIN",
//                        "admin/alerts/super-admin-role-revoked",
//                        templateData
//                );
//            }
//        }
//    }
//
//    /**
//     * Envoie une alerte pour la désactivation d'utilisateur sensible.
//     */
//    private void sendSuperAdminDeactivationAlert(AdminUserDeactivatedEvent event) {
//        List<User> superAdmins = userService.getUsersByRole(UserRole.SUPER_ADMIN);
//
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("targetUsername", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("category", event.category());
//        templateData.put("reason", event.category().getDisplayName());
//        templateData.put("comment", event.comment());
//        templateData.put("isAdministrativeUser", event.isAdministrativeUser());
//        templateData.put("severityLevel", event.getSeverityLevel());
//
//        for (User superAdmin : superAdmins) {
//            if (!superAdmin.getId().equals(event.getAdminUserId())) {
//                mailerService.sendTemplateEmail(
//                        superAdmin.getEmail(),
//                        "[ALERTE] Désactivation d'utilisateur sensible",
//                        "admin/alerts/sensitive-user-deactivated",
//                        templateData
//                );
//            }
//        }
//    }
//
//    /**
//     * Envoie une alerte pour reset de mot de passe urgent.
//     */
//    private void sendSuperAdminPasswordResetAlert(AdminPasswordResetTriggeredEvent event) {
//        List<User> superAdmins = userService.getUsersByRole(UserRole.SUPER_ADMIN);
//
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("targetUsername", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("reason", event.reason());
//        templateData.put("priorityLevel", event.getPriorityLevel());
//        templateData.put("isAdministrativeUser", event.isAdministrativeUser());
//
//        for (User superAdmin : superAdmins) {
//            if (!superAdmin.getId().equals(event.getAdminUserId())) {
//                mailerService.sendTemplateEmail(
//                        superAdmin.getEmail(),
//                        "[ALERTE] Reset de mot de passe urgent",
//                        "admin/alerts/urgent-password-reset",
//                        templateData
//                );
//            }
//        }
//    }
//
//    /**
//     * Envoie une alerte pour échec d'action de sécurité.
//     */
//    private void sendSecurityActionFailureAlert(AdminActionEvent event) {
//        List<User> superAdmins = userService.getUsersByRole(UserRole.SUPER_ADMIN);
//
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("actionType", event.actionType());
//        templateData.put("actionDescription", event.actionDescription());
//        templateData.put("errorMessage", event.resultMessage());
//        templateData.put("targetUsername", event.getTargetUsername());
//
//        for (User superAdmin : superAdmins) {
//            if (!superAdmin.getId().equals(event.getAdminUserId())) {
//                mailerService.sendTemplateEmail(
//                        superAdmin.getEmail(),
//                        "[ALERTE SÉCURITÉ] Échec d'action administrative critique",
//                        "admin/alerts/security-action-failure",
//                        templateData
//                );
//            }
//        }
//    }
//
//    /**
//     * Envoie une notification spéciale pour attribution de rôle administratif.
//     */
//    private void sendAdministrativeRoleGrantedNotification(UserRoleGrantedEvent event) {
//        if (event.isFirstAdministrativeRole()) {
//            Map<String, Object> templateData = new HashMap<>();
//            templateData.put("username", event.getTargetUsername());
//            templateData.put("grantedRole", event.grantedRole());
//            templateData.put("adminUsername", event.getAdminUsername());
//
//            mailerService.sendTemplateEmail(
//                    event.targetUser().getEmail(),
//                    "Privilèges d'administration accordés",
//                    "admin/first-administrative-role-granted",
//                    templateData
//            );
//        }
//    }
//
//    /**
//     * Envoie une notification pour perte de tous les privilèges administratifs.
//     */
//    private void sendAdministrativePrivilegesLostNotification(UserRoleRevokedEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("reason", event.reason());
//
//        mailerService.sendTemplateEmail(
//                event.targetUser().getEmail(),
//                "Privilèges d'administration révoqués",
//                "admin/administrative-privileges-lost",
//                templateData
//        );
//    }
//
//    /**
//     * Envoie une notification spéciale pour réactivation.
//     */
//    private void sendUserReactivationNotification(AdminUserActivatedEvent event) {
//        Map<String, Object> templateData = new HashMap<>();
//        templateData.put("username", event.getTargetUsername());
//        templateData.put("adminUsername", event.getAdminUsername());
//        templateData.put("deactivationDurationDays", event.getDeactivationDurationMs() / (1000 * 60 * 60 * 24));
//
//        mailerService.sendTemplateEmail(
//                event.targetUser().getEmail(),
//                "Bienvenue de retour - Votre compte a été réactivé",
//                "admin/welcome-back-reactivation",
//                templateData
//        );
//    }
//}