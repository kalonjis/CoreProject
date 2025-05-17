package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.bll.utils.device.IpUtils;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implémentation du service ActivityLogService qui utilise une approche hybride
 * pour la gestion des exceptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Méthode générique pour enregistrer une action utilisateur.
     * Capture les exceptions liées au formatage JSON mais laisse remonter
     * les exceptions métier pour être traitées par le ControllerAdvisor.
     */
    @Override
    @Transactional
    public ActivityLog logUserAction(
            User user,
            Device device,
            ActionLogType actionType,
            boolean successful,
            String details,
            String metadata,
            RequestContext requestContext) {

        String clientIp = getClientIpAddress(requestContext);
        String location = IpUtils.getLocationFromIp(clientIp);
        String sessionId = getOrCreateSessionId(requestContext);

        // Évaluer le niveau de risque en fonction du type d'action et du contexte
        int riskLevel = evaluateRiskLevel(user, device, actionType, requestContext);
        boolean triggeredAlert = riskLevel >= 3; // Alerte si niveau de risque élevé

        if (triggeredAlert) {
            log.warn("Action à risque détectée: {} pour l'utilisateur {}, niveau de risque {}",
                    actionType, user.getUsername(), riskLevel);
        }

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .device(device)
                .timestamp(Instant.now())
                .ipAddress(clientIp)
                .location(location)
                .successful(successful)
                .failureReason(successful ? null : details)
                .actionType(actionType.name())
                .actionDetails(details)
                .sessionId(sessionId)
                .metadata(metadata)
                .riskLevel(riskLevel)
                .triggeredAlert(triggeredAlert)
                .build();

        ActivityLog savedLog = activityLogRepository.save(activityLog);

        // Si l'action a déclenché une alerte, effectuer des actions supplémentaires
        if (triggeredAlert) {
            handleSecurityAlert(savedLog);
        }

        return savedLog;
    }

    // region Connexion

    /**
     * Enregistre une tentative de connexion
     */
    @Override
    @Transactional
    public ActivityLog logLogin(User user, Device device, boolean successful,
                                String failureReason, RequestContext requestContext) {

        ActionLogType actionType = successful ? ActionLogType.AUTH_LOGIN : ActionLogType.AUTH_LOGIN_FAILED;
        String metadataJson = null;

        try {
            // Formatage de métadonnées - gérons cette exception spécifique localement
            // car elle n'est pas critique pour le flux principal
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userAgent", requestContext.getUserAgent());
            metadata.put("referrer", requestContext.getHeaders().getReferer());
            metadata.put("method", "FORM"); // ou "SSO", "API", etc.

            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            // Exception de formatage JSON - on peut la gérer ici sans interrompre le flux
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        // Les exceptions de la méthode logUserAction remonteront naturellement
        // pour être traitées par le ControllerAdvisor
        return logUserAction(
                user,
                device,
                actionType,
                successful,
                failureReason,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre une déconnexion
     */
    @Override
    @Transactional
    public ActivityLog logLogout(User user, Device device, RequestContext requestContext) {
        // Calculer la durée de la session si possible
//        Long sessionDuration = calculateSessionDuration(requestContext);

        ActivityLog log = logUserAction(
                user,
                device,
                ActionLogType.AUTH_LOGOUT,
                true,
                null,
                null,
                requestContext
        );

        // Mettre à jour la durée si disponible
        if (requestContext.getSessionDurationSeconds() != null) {
            log.setDurationSeconds(requestContext.getSessionDurationSeconds());
            activityLogRepository.save(log);
        }

        return log;
    }

    // endregion

    // region Password

    /**
     * Enregistre un changement de mot de passe
     */
    @Override
    @Transactional
    public ActivityLog logPasswordChange(User user, Device device, boolean successful, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.PASSWORD_CHANGED,
                successful,
                successful ? "Mot de passe modifié avec succès" : "Échec de la modification du mot de passe",
                null,
                requestContext
        );
    }

    /**
     * Enregistre une demande de réinitialisation de mot de passe
     */
    @Override
    @Transactional
    public ActivityLog logPasswordResetRequest(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.PASSWORD_RESET_REQUEST,
                true,
                "Demande de réinitialisation de mot de passe effectuée",
                null,
                requestContext
        );
    }

    /**
     * Enregistre une demande de réinitialisation de mot de passe
     */
    @Override
    @Transactional
    public ActivityLog logRequestPasswordToken(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.PASSWORD_REQUEST_TOKEN,
                true,
                "Nouvelle demande de réinitialisation de mot de passe effectuée",
                null,
                requestContext
        );
    }

    /**
     * Enregistre une réinitialisation complète de mot de passe
     */
    @Override
    @Transactional
    public ActivityLog logPasswordResetComplete(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.PASSWORD_RESET_COMPLETE,
                true,
                "Réinitialisation de mot de passe effectuée",
                null,
                requestContext
        );
    }

    // endregion

    // region Email


    /**
     * Enregistre une demande de changement d'email
     */
    @Override
    @Transactional
    public ActivityLog logEmailChangeRequest(User user, Device device, String newEmail, RequestContext requestContext) {
        String metadataJson = null;

        try {
            // Exception non critique - on peut la gérer localement
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("newEmail", newEmail);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        return logUserAction(
                user,
                device,
                ActionLogType.EMAIL_CHANGE_REQUEST,
                true,
                "Demande de changement d'email de " + user.getEmail() + " vers " + newEmail,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre la confirmation d'un changement d'email
     */
    @Override
    @Transactional
    public ActivityLog logEmailChangeComplete(User user, Device device, String oldEmail, String newEmail, RequestContext requestContext) {
        String metadataJson = null;

        try {
            // Exception non critique - on peut la gérer localement
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oldEmail", oldEmail);
            metadata.put("newEmail", newEmail);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        return logUserAction(
                user,
                device,
                ActionLogType.EMAIL_CHANGE_COMPLETE,
                true,
                "Changement d'email de " + oldEmail + " vers " + newEmail + " effectué",
                metadataJson,
                requestContext
        );
    }

    // endregion

    // region Account

    /**
     * Enregistre la création d'un compte utilisateur
     */
    @Override
    @Transactional
    public ActivityLog logAccountCreation(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_CREATED,
                true,
                "Création du compte utilisateur",
                null,
                requestContext
        );
    }


    /**
     * Enregistre l'activation d'un nouveau compte utilisateur (1ere activation)
     */
    @Override
    @Transactional
    public ActivityLog logNewAccountActivation(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_ACTIVATED,
                true,
                "Activation du nouveau compte utilisateur",
                null,
                requestContext
        );
    }


    /**
     * Enregistre l'activation d'un compte utilisateur (apres une désactivation)
     */
    @Override
    @Transactional
    public ActivityLog logAccountActivation(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_ACTIVATED,
                true,
                "Réactivation du compte utilisateur",
                null,
                requestContext
        );
    }

    /**
     * Enregistre la désactivation d'un compte utilisateur
     */
    @Override
    @Transactional
    public ActivityLog logAccountDeactivation(User user, Long adminId, RequestContext requestContext) {
        String metadataJson = null;

        try {
            // Exception non critique - on peut la gérer localement
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("adminId", adminId);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        return logUserAction(
                user,
                null,
                ActionLogType.ACCOUNT_DEACTIVATED,
                true,
                "Désactivation du compte utilisateur par l'administrateur #" + adminId,
                metadataJson,
                requestContext
        );
    }

    // endregion

    /**
     * Enregistre un changement de rôle utilisateur
     */
    @Override
    @Transactional
    public ActivityLog logRoleChange(User user, String role, boolean granted, Long adminId, RequestContext requestContext) {
        ActionLogType actionType = granted ? ActionLogType.ROLE_GRANTED : ActionLogType.ROLE_REVOKED;

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("adminId", adminId);
            metadata.put("role", role);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        String details = granted ?
                "Attribution du rôle " + role + " par l'administrateur #" + adminId :
                "Révocation du rôle " + role + " par l'administrateur #" + adminId;

        return logUserAction(
                user,
                null,
                actionType,
                true,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre un enregistrement d'appareil
     */
    @Override
    @Transactional
    public ActivityLog logDeviceRegistration(User user, Device device, RequestContext requestContext) {
        String metadataJson = null;

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("deviceType", device.getDeviceType());
            metadata.put("browser", device.getBrowser());
            metadata.put("os", device.getOperatingSystem());
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        return logUserAction(
                user,
                device,
                ActionLogType.DEVICE_REGISTERED,
                true,
                "Nouvel appareil enregistré: " + device.getDeviceType() + " - " + device.getBrowser(),
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre une confirmation d'appareil
     */
    @Override
    @Transactional
    public ActivityLog logDeviceConfirmation(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.DEVICE_CONFIRMED,
                true,
                "Appareil confirmé: " + device.getDeviceType() + " - " + device.getBrowser(),
                null,
                requestContext
        );
    }

    /**
     * Enregistre un rejet d'appareil
     */
    @Override
    @Transactional
    public ActivityLog logDeviceRejection(User user, Device device, RequestContext requestContext) {
        return logUserAction(
                user,
                device,
                ActionLogType.DEVICE_REJECTED,
                true,
                "Appareil rejeté: " + device.getDeviceType() + " - " + device.getBrowser(),
                null,
                requestContext
        );
    }

    /**
     * Enregistre un changement de niveau de confiance d'appareil
     */
    @Override
    @Transactional
    public ActivityLog logDeviceTrustLevelChange(User user, Device device, String oldLevel, String newLevel, RequestContext requestContext) {
        String metadataJson = null;

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oldLevel", oldLevel);
            metadata.put("newLevel", newLevel);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        return logUserAction(
                user,
                device,
                ActionLogType.DEVICE_TRUST_LEVEL_CHANGED,
                true,
                "Niveau de confiance de l'appareil modifié de " + oldLevel + " à " + newLevel,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre une activité suspecte
     */
    @Override
    @Transactional
    public ActivityLog logSuspiciousActivity(User user, Device device, String details, int riskLevel, RequestContext requestContext) {
        String metadataJson = null;

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("riskLevel", riskLevel);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des métadonnées: {}", e.getMessage());
            metadataJson = "{}";
        }

        return logUserAction(
                user,
                device,
                ActionLogType.SECURITY_SUSPICIOUS_ACTIVITY,
                true,
                details,
                metadataJson,
                requestContext
        );
    }

    // -------- Méthodes de recherche et d'analyse --------
    // Ces méthodes peuvent laisser remonter naturellement les exceptions
    // pour être traitées par le ControllerAdvisor

    /**
     * Obtient l'historique des connexions d'un utilisateur
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserConnectionHistory(User user, Pageable pageable) {
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    /**
     * Obtient l'historique des actions d'un utilisateur
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserActionHistory(User user, List<ActionLogType> actionTypes,
                                                  Instant startDate, Instant endDate, Pageable pageable) {
        List<String> actionTypeStrings = actionTypes != null ?
                actionTypes.stream().map(ActionLogType::name).collect(Collectors.toList()) : null;

        return activityLogRepository.findByUserAndActionTypeInAndTimestampBetween(
                user, actionTypeStrings, startDate, endDate, pageable);
    }

    /**
     * Obtient les dernières tentatives de connexion d'un utilisateur
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentLoginAttempts(User user) {
        return activityLogRepository.findTop10ByUserAndActionTypeOrderByTimestampDesc(
                user, ActionLogType.AUTH_LOGIN.name());
    }

    /**
     * Rechercher des activités par critères
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> searchLogs(Long userId, String ipAddress, List<ActionLogType> actionTypes,
                                        Boolean successful, Instant startDate, Instant endDate, Pageable pageable) {
        List<String> actionTypeStrings = actionTypes != null ?
                actionTypes.stream().map(ActionLogType::name).collect(Collectors.toList()) : null;

        return activityLogRepository.searchLogs(userId, ipAddress, actionTypeStrings,
                successful, startDate, endDate, pageable);
    }

    /**
     * Obtient des statistiques sur les activités utilisateur
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserActivityStats(User user, Instant startDate, Instant endDate) {
        Map<String, Object> stats = new HashMap<>();

        // Nombre total de connexions
        Long totalLogins = activityLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampBetween(
                user, ActionLogType.AUTH_LOGIN.name(), true, startDate, endDate);
        stats.put("totalLogins", totalLogins);

        // Nombre de tentatives de connexion échouées
        Long failedLogins = activityLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampBetween(
                user, ActionLogType.AUTH_LOGIN.name(), false, startDate, endDate);
        stats.put("failedLogins", failedLogins);

        // Dernière connexion réussie
        ActivityLog lastLogin = activityLogRepository.findTopByUserAndActionTypeAndSuccessfulOrderByTimestampDesc(
                user, ActionLogType.AUTH_LOGIN.name(), true);
        stats.put("lastLoginTime", lastLogin != null ? lastLogin.getTimestamp() : null);
        stats.put("lastLoginLocation", lastLogin != null ? lastLogin.getLocation() : null);

        // Nombre de changements de mot de passe
        Long passwordChanges = activityLogRepository.countByUserAndActionTypeAndTimestampBetween(
                user, ActionLogType.PASSWORD_CHANGED.name(), startDate, endDate);
        stats.put("passwordChanges", passwordChanges);

        // Nombre d'appareils distincts utilisés
        Long distinctDevices = activityLogRepository.countDistinctDevicesByUserAndTimestampBetween(
                user.getId(), startDate, endDate);
        stats.put("distinctDevices", distinctDevices);

        // Liste des adresses IP utilisées
        List<String> ipAddresses = activityLogRepository.findDistinctIpAddressesByUserAndTimestampBetween(
                user, startDate, endDate);
        stats.put("ipAddresses", ipAddresses);

        return stats;
    }

    /**
     * Obtient des statistiques globales de connexion
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemLoginStats(Instant startDate, Instant endDate) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Nombre total de connexions
            Long totalLogins = activityLogRepository.countByActionTypeAndSuccessfulAndTimestampBetween(
                    ActionLogType.AUTH_LOGIN.name(), true, startDate, endDate);
            stats.put("totalLogins", totalLogins);

            // Nombre total de tentatives de connexion échouées
            Long failedLogins = activityLogRepository.countByActionTypeAndSuccessfulAndTimestampBetween(
                    ActionLogType.AUTH_LOGIN.name(), false, startDate, endDate);
            stats.put("failedLogins", failedLogins);

            // Nombre d'utilisateurs uniques connectés
            Long uniqueUsers = activityLogRepository.countDistinctUsersByActionTypeAndTimestampBetween(
                    ActionLogType.AUTH_LOGIN.name(), startDate, endDate);
            stats.put("uniqueUsers", uniqueUsers);

            // Nombre d'adresses IP uniques
            Long uniqueIPs = activityLogRepository.countDistinctIpAddressesByActionTypeAndTimestampBetween(
                    ActionLogType.AUTH_LOGIN.name(), startDate, endDate);
            stats.put("uniqueIPs", uniqueIPs);
        } catch (Exception e) {
            // Cette méthode compile beaucoup de requêtes et est utilisée pour l'UI administrateur
            // Une exception ici ne devrait pas bloquer l'application, alors on la gère localement
            log.error("Erreur lors de la génération des statistiques système: {}", e.getMessage(), e);
            stats.put("error", "Erreur lors de la génération des statistiques");

            // On utilise une exception personnalisée pour être cohérent avec le système d'erreurs
            throw new CoreProjectException("Erreur lors de la génération des statistiques système", 500);
        }

        return stats;
    }

    /**
     * Obtient la répartition géographique des connexions
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getLoginsByLocation(Instant startDate, Instant endDate) {
        List<Object[]> results = activityLogRepository.countLoginsByLocation(startDate, endDate);
        Map<String, Long> locationMap = new HashMap<>();

        for (Object[] result : results) {
            String location = (String) result[0];
            Long count = (Long) result[1];
            locationMap.put(location != null ? location : "Unknown", count);
        }

        return locationMap;
    }

    /**
     * Obtient le nombre de connexions par jour
     */
    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> getLoginsByDay(Instant startDate, Instant endDate) {
        List<Object[]> results = activityLogRepository.countLoginsByDay(startDate, endDate);
        Map<LocalDate, Long> loginsByDay = new HashMap<>();

        for (Object[] result : results) {
            java.sql.Date date = (java.sql.Date) result[0];
            Long count = (Long) result[1];
            LocalDate localDate = date.toLocalDate();
            loginsByDay.put(localDate, count);
        }

        return loginsByDay;
    }

    /**
     * Détecte les activités suspectes pour un utilisateur
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> detectSuspiciousActivity(User user) {
        // Cette méthode peut générer des exceptions complexes lors de l'analyse
        // C'est un bon candidat pour une gestion d'erreurs locale
        try {
            // Période à considérer pour l'analyse (les 7 derniers jours)
            Instant startDate = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant endDate = Instant.now();

            // Activités suspectes: connexions depuis des emplacements différents en peu de temps
            return activityLogRepository.findSuspiciousActivities(user, startDate, endDate);
        } catch (Exception e) {
            log.error("Erreur lors de la détection d'activités suspectes pour l'utilisateur {}: {}",
                    user.getUsername(), e.getMessage(), e);

            // On renvoie une liste vide plutôt que de faire échouer l'application
            return Collections.emptyList();
        }
    }

    /**
     * Tâche planifiée pour supprimer les anciens logs
     * Les exceptions sont gérées localement car cette méthode
     * s'exécute automatiquement sans intervention utilisateur.
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?") // Tous les jours à minuit
    public void cleanupOldLogs() {
        try {
            // Par défaut, conserve 6 mois de logs
            Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
            long deletedCount = activityLogRepository.deleteByTimestampBefore(cutoffDate);
            log.info("Nettoyage des logs de connexion terminé: {} enregistrements supprimés (antérieurs à {})",
                    deletedCount, cutoffDate);
        } catch (Exception e) {
            // Une tâche planifiée ne doit pas faire planter l'application
            log.error("Erreur lors du nettoyage des anciens logs: {}", e.getMessage(), e);
        }
    }

    /**
     * Détermine l'adresse IP du client à partir de la requête HTTP
     */
    private String getClientIpAddress(RequestContext requestContext) {
        String xForwardedFor = requestContext.getHeaders().getXForwardedFor();
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // En cas de plusieurs proxies, la première IP est celle du client
            return xForwardedFor.split(",")[0].trim();
        }
        return requestContext.getClientIp();
    }

    /**
     * Récupère ou crée un identifiant de session
     */
    private String getOrCreateSessionId(RequestContext requestContext) {
        if (requestContext.getSessionId() != null) {
            return requestContext.getSessionId();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Calcule la durée de la session en secondes
     */
//    private Long calculateSessionDuration(RequestContext requestContext) {
//        HttpSession session = requestContext.getSession(false);
//        if (session != null) {
//            long creationTime = session.getCreationTime();
//            long now = System.currentTimeMillis();
//            return (now - creationTime) / 1000; // Conversion en secondes
//        }
//        return null;
//    }

    /**
     * Évalue le niveau de risque d'une action (0-3)
     */
    private int evaluateRiskLevel(User user, Device device, ActionLogType actionType, RequestContext requestContext) {
        int riskLevel = 0;

        // Facteurs augmentant le risque

        // 1. Si l'action est sensible
        if (actionType == ActionLogType.PASSWORD_CHANGED ||
                actionType == ActionLogType.PASSWORD_RESET_COMPLETE ||
                actionType == ActionLogType.EMAIL_CHANGE_COMPLETE) {
            riskLevel += 1;
        }

        // 2. Si l'appareil n'est pas reconnu ou n'est pas confirmé
        if (device == null || !device.isConfirmed()) {
            riskLevel += 1;
        }

        // 3. Si l'adresse IP est nouvelle pour cet utilisateur
        String ipAddress = getClientIpAddress(requestContext);
        boolean ipKnown = activityLogRepository.existsByUserAndIpAddressAndTimestampAfter(
                user, ipAddress, Instant.now().minus(30, ChronoUnit.DAYS));
        if (!ipKnown) {
            riskLevel += 1;
        }

        // 4. Si la localisation est inhabituelle
        String location = IpUtils.getLocationFromIp(ipAddress);
        boolean locationKnown = activityLogRepository.existsByUserAndLocationAndTimestampAfter(
                user, location, Instant.now().minus(30, ChronoUnit.DAYS));
        if (!locationKnown && location != null && !location.equals("Unknown")) {
            riskLevel += 1;
        }

        // Limiter le niveau de risque à 3 (maximum)
        return Math.min(riskLevel, 3);
    }

    /**
     * Gère une alerte de sécurité
     */
    private void handleSecurityAlert(ActivityLog activityLog) {
        // Ici, on pourrait implémenter:
        // 1. Envoi d'email à l'utilisateur
        // 2. Notification d'un administrateur
        // 3. Blocage temporaire du compte
        // 4. Journalisation dans un système de monitoring

        log.info("Alerte de sécurité déclenchée: {} pour l'utilisateur {} depuis {}",
                activityLog.getActionType(), activityLog.getUser().getUsername(), activityLog.getIpAddress());
    }
}