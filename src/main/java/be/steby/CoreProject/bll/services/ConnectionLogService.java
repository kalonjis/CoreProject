package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.dl.entities.ConnectionLog;
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

public interface ConnectionLogService {

    /**
     * Méthode générique pour enregistrer une action utilisateur
     */
    ConnectionLog logUserAction(
            User user,
            Device device,
            ActionLogType actionType,
            boolean successful,
            String details,
            String metadata,
            HttpServletRequest request
    );

    /**
     * Enregistre une tentative de connexion
     */
    ConnectionLog logLogin(
            User user,
            Device device,
            boolean successful,
            String failureReason,
            HttpServletRequest request
    );

    /**
     * Enregistre une déconnexion
     */
    ConnectionLog logLogout(
            User user,
            Device device,
            HttpServletRequest request
    );

    /**
     * Enregistre un changement de mot de passe
     */
    ConnectionLog logPasswordChange(
            User user,
            Device device,
            boolean successful,
            HttpServletRequest request
    );

    /**
     * Enregistre une demande de réinitialisation de mot de passe
     */
    ConnectionLog logPasswordResetRequest(
            User user,
            HttpServletRequest request
    );

    /**
     * Enregistre une réinitialisation complète de mot de passe
     */
    ConnectionLog logPasswordResetComplete(
            User user,
            Device device,
            HttpServletRequest request
    );

    /**
     * Enregistre une demande de changement d'email
     */
    ConnectionLog logEmailChangeRequest(
            User user,
            Device device,
            String newEmail,
            HttpServletRequest request
    );

    /**
     * Enregistre la confirmation d'un changement d'email
     */
    ConnectionLog logEmailChangeComplete(
            User user,
            Device device,
            String oldEmail,
            String newEmail,
            HttpServletRequest request
    );

    /**
     * Enregistre la création d'un compte utilisateur
     */
    ConnectionLog logAccountCreation(
            User user,
            Device device,
            HttpServletRequest request
    );

    /**
     * Enregistre l'activation d'un compte utilisateur
     */
    ConnectionLog logAccountActivation(
            User user,
            Device device,
            HttpServletRequest request
    );

    /**
     * Enregistre la désactivation d'un compte utilisateur
     */
    ConnectionLog logAccountDeactivation(
            User user,
            Long adminId,
            HttpServletRequest request
    );

    /**
     * Enregistre un changement de rôle utilisateur
     */
    ConnectionLog logRoleChange(
            User user,
            String role,
            boolean granted,
            Long adminId,
            HttpServletRequest request
    );

    /**
     * Enregistre un enregistrement d'appareil
     */
    ConnectionLog logDeviceRegistration(
            User user,
            Device device,
            HttpServletRequest request
    );

    /**
     * Enregistre une confirmation d'appareil
     */
    ConnectionLog logDeviceConfirmation(
            User user,
            Device device,
            HttpServletRequest request
    );

    /**
     * Enregistre un rejet d'appareil
     */
    ConnectionLog logDeviceRejection(
            User user,
            Device device,
            HttpServletRequest request
    );

    /**
     * Enregistre un changement de niveau de confiance d'appareil
     */
    ConnectionLog logDeviceTrustLevelChange(
            User user,
            Device device,
            String oldLevel,
            String newLevel,
            HttpServletRequest request
    );

    /**
     * Enregistre une activité suspecte
     */
    ConnectionLog logSuspiciousActivity(
            User user,
            Device device,
            String details,
            int riskLevel,
            HttpServletRequest request
    );

    /**
     * Obtient l'historique des connexions d'un utilisateur
     */
    Page<ConnectionLog> getUserConnectionHistory(
            User user,
            Pageable pageable
    );

    /**
     * Obtient l'historique des actions d'un utilisateur
     */
    Page<ConnectionLog> getUserActionHistory(
            User user,
            List<ActionLogType> actionTypes,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );

    /**
     * Obtient les dernières tentatives de connexion d'un utilisateur
     */
    List<ConnectionLog> getRecentLoginAttempts(User user);

    /**
     * Rechercher des activités par critères
     */
    Page<ConnectionLog> searchLogs(
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
    List<ConnectionLog> detectSuspiciousActivity(User user);

    /**
     * Nettoie les anciens logs
     */
    void cleanupOldLogs();
}