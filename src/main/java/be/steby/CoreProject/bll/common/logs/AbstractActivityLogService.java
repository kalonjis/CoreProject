package be.steby.CoreProject.bll.common.logs;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.domains.device.utils.IpUtils;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe abstraite fournissant les fonctionnalités de base pour la journalisation des activités utilisateur.
 * Cette classe est conçue pour être étendue par des services spécifiques à chaque domaine.
 */
@Slf4j
public abstract class AbstractActivityLogService {

    protected final ActivityLogRepository activityLogRepository;
    protected final ObjectMapper objectMapper;

    /**
     * Constructeur pour l'initialisation des dépendances communes.
     *
     * @param activityLogRepository Repository pour la persistance des logs d'activité
     */
    protected AbstractActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Méthode principale pour l'enregistrement d'une action utilisateur.
     * Cette méthode est utilisée par toutes les méthodes spécifiques de journalisation.
     *
     * @param user L'utilisateur qui a effectué l'action
     * @param device L'appareil utilisé (peut être null)
     * @param actionType Le type d'action
     * @param successful Si l'action a réussi ou échoué
     * @param details Détails supplémentaires sur l'action
     * @param metadata Métadonnées au format JSON (peut être null)
     * @param requestContext Contexte de la requête
     * @return L'entrée de journal créée
     */
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
                .actionType(actionType)
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

    // ================== Méthodes de recherche et d'analyse ==================

    /**
     * Obtient l'historique des connexions d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserConnectionHistory(User user, Pageable pageable) {
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    /**
     * Obtient l'historique des actions d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserActionHistory(User user, List<ActionLogType> actionTypes,
                                                  Instant startDate, Instant endDate, Pageable pageable) {
        List<String> actionTypeStrings = actionTypes != null ?
                actionTypes.stream().map(ActionLogType::name).collect(Collectors.toList()) : null;

        return activityLogRepository.findByUserAndActionTypeInAndTimestampBetween(
                user, actionTypes, startDate, endDate, pageable);
    }

    /**
     * Rechercher des activités par critères
     */
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
    @Scheduled(cron = "0 0 0 * * ?") // Tous les jours à minuit
    @Transactional
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

    // ================== Méthodes protégées utilitaires ==================

    /**
     * Détermine l'adresse IP du client à partir du contexte de requête
     */
    protected String getClientIpAddress(RequestContext requestContext) {
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
    protected String getOrCreateSessionId(RequestContext requestContext) {
        if (requestContext.getSessionId() != null) {
            return requestContext.getSessionId();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Évalue le niveau de risque d'une action (0-3)
     */
    protected int evaluateRiskLevel(User user, Device device, ActionLogType actionType, RequestContext requestContext) {
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
    protected void handleSecurityAlert(ActivityLog activityLog) {
        // Ici, on pourrait implémenter:
        // 1. Envoi d'email à l'utilisateur
        // 2. Notification d'un administrateur
        // 3. Blocage temporaire du compte
        // 4. Journalisation dans un système de monitoring

        log.info("Alerte de sécurité déclenchée: {} pour l'utilisateur {} depuis {}",
                activityLog.getActionType(), activityLog.getUser().getUsername(), activityLog.getIpAddress());
    }

    /**
     * Obtient les dernières tentatives de connexion réussies d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentSuccessfulLoginAttempts(User user, int limit) {
        return activityLogRepository.findTop10ByUserAndActionTypeOrderByTimestampDesc(
                user, ActionLogType.AUTH_LOGIN.name());
    }

    /**
     * Retourne le nom du domaine pour ce service.
     * Cette méthode est utilisée principalement pour le logging et peut être surchargée
     * par les services spécifiques.
     *
     * @return Le nom du domaine correspondant au service
     */
    protected String getDomainName() {
        return "GENERAL";
    }
}