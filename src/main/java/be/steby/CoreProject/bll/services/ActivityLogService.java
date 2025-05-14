package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ActivityLogService {

    /**
     * Méthode générique pour enregistrer une action utilisateur
     */
    ActivityLog logUserAction(
            User user,
            Device device,
            ActionLogType actionType,
            boolean successful,
            String details,
            String metadata,
            RequestContext requestContext
    );

    /**
     * Enregistre une tentative de connexion
     */
    ActivityLog logLogin(
            User user,
            Device device,
            boolean successful,
            String failureReason,
            RequestContext requestContext
    );

    /**
     * Enregistre une déconnexion
     */
    ActivityLog logLogout(
            User user,
            Device device,
            RequestContext requestContext
    );

    /**
     * Enregistre un changement de mot de passe
     */
    ActivityLog logPasswordChange(
            User user,
            Device device,
            boolean successful,
            RequestContext requestContext
    );

    /**
     * Enregistre une demande de réinitialisation de mot de passe
     */
    ActivityLog logPasswordResetRequest(
            User user,
            Device device,
            RequestContext requestContext
    );

    /**
     * Enregistre une réinitialisation complète de mot de passe
     */
    ActivityLog logPasswordResetComplete(
            User user,
            Device device,
            RequestContext requestContext
    );

    /**
     * Enregistre une demande de changement d'email
     */
    ActivityLog logEmailChangeRequest(
            User user,
            Device device,
            String newEmail,
            RequestContext requestContext
    );

    /**
     * Enregistre la confirmation d'un changement d'email
     */
    ActivityLog logEmailChangeComplete(
            User user,
            Device device,
            String oldEmail,
            String newEmail,
            RequestContext requestContext
    );

    /**
     * Enregistre la création d'un compte utilisateur
     */
    ActivityLog logAccountCreation(
            User user,
            Device device,
            RequestContext requestContext
    );


    /**
     * Enregistre l'activation d'un compte d'un nouvel utilisateur (1ere activation)
     */
    ActivityLog logNewAccountActivation(User user, Device device, RequestContext requestContext);

    /**
     * Enregistre l'activation d'un compte utilisateur (après une désactivation)
     */
    ActivityLog logAccountActivation(
            User user,
            Device device,
            RequestContext requestContext
    );

    /**
     * Enregistre la désactivation d'un compte utilisateur
     */
    ActivityLog logAccountDeactivation(
            User user,
            Long adminId,
            RequestContext requestContext
    );

    /**
     * Enregistre un changement de rôle utilisateur
     */
    ActivityLog logRoleChange(
            User user,
            String role,
            boolean granted,
            Long adminId,
            RequestContext requestContext
    );

    /**
     * Enregistre un enregistrement d'appareil
     */
    ActivityLog logDeviceRegistration(
            User user,
            Device device,
            RequestContext requestContext
    );

    /**
     * Enregistre une confirmation d'appareil
     */
    ActivityLog logDeviceConfirmation(
            User user,
            Device device,
            RequestContext requestContext
    );

    /**
     * Enregistre un rejet d'appareil
     */
    ActivityLog logDeviceRejection(
            User user,
            Device device,
            RequestContext requestContext
    );

    /**
     * Enregistre un changement de niveau de confiance d'appareil
     */
    ActivityLog logDeviceTrustLevelChange(
            User user,
            Device device,
            String oldLevel,
            String newLevel,
            RequestContext requestContext
    );

    /**
     * Enregistre une activité suspecte
     */
    ActivityLog logSuspiciousActivity(
            User user,
            Device device,
            String details,
            int riskLevel,
            RequestContext requestContext
    );

    /**
     * Obtient l'historique des connexions d'un utilisateur
     */
    Page<ActivityLog> getUserConnectionHistory(
            User user,
            Pageable pageable
    );

    /**
     * Obtient l'historique des actions d'un utilisateur
     */
    Page<ActivityLog> getUserActionHistory(
            User user,
            List<ActionLogType> actionTypes,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );

    /**
     * Obtient les dernières tentatives de connexion d'un utilisateur
     */
    List<ActivityLog> getRecentLoginAttempts(User user);

    /**
     * Rechercher des activités par critères
     */
    Page<ActivityLog> searchLogs(
            Long userId,
            String ipAddress,
            List<ActionLogType> actionTypes,
            Boolean successful,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );

    /**
     * Obtient des statistiques sur les activités utilisateur
     */
    Map<String, Object> getUserActivityStats(
            User user,
            Instant startDate,
            Instant endDate
    );

    /**
     * Obtient des statistiques globales de connexion
     */
    Map<String, Object> getSystemLoginStats(
            Instant startDate,
            Instant endDate
    );

    /**
     * Obtient la répartition géographique des connexions
     */
    Map<String, Long> getLoginsByLocation(
            Instant startDate,
            Instant endDate
    );

    /**
     * Obtient le nombre de connexions par jour
     */
    Map<LocalDate, Long> getLoginsByDay(
            Instant startDate,
            Instant endDate
    );

    /**
     * Détecte les activités suspectes pour un utilisateur
     */
    List<ActivityLog> detectSuspiciousActivity(User user);

    /**
     * Nettoie les anciens logs
     */
    void cleanupOldLogs();
}