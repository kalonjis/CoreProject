package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.activitylog.AbstractActivityLogService;
import be.steby.CoreProject.bll.domains.admin.events.AdminActionEvent;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service de journalisation spécifique aux opérations d'administration.
 * Cette classe s'occupe de l'enregistrement et l'analyse des activités
 * d'administration avec une meilleure granularité et des métadonnées spécialisées.
 */
@Service
@Slf4j
public class AdminActivityLogService extends AbstractActivityLogService {

    /**
     * Constructeur pour l'initialisation des dépendances.
     *
     * @param activityLogRepository Repository pour la persistance des logs d'activité
     */
    public AdminActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    /**
     * Retourne le nom du domaine pour ce service.
     *
     * @return Le nom du domaine "ADMIN"
     */
    @Override
    protected String getDomainName() {
        return "ADMIN";
    }

    @Override
    protected int getBaseRiskForActionType(ActionLogType actionType) {
        return 0;
    }

    // ================== MÉTHODES SPÉCIALISÉES D'ENREGISTREMENT ==================

    /**
     * Enregistre une action de création d'utilisateur par un administrateur.
     */
    @Transactional
    public void logUserCreation(User adminUser, User createdUser, Set<UserRole> assignedRoles,
                                boolean autoActivated, RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("createdUserId", createdUser.getId());
            metadata.put("createdUsername", createdUser.getUsername());
            metadata.put("createdUserEmail", createdUser.getEmail());
            metadata.put("assignedRoles", assignedRoles);
            metadata.put("autoActivated", autoActivated);
            metadata.put("hasAdminRoles", assignedRoles.stream().anyMatch(this::isAdministrativeRole));

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ADMIN_USER_CREATED, // ← Enum directement !
                    true, // successful
                    String.format("Création de l'utilisateur '%s' avec les rôles: %s",
                            createdUser.getUsername(), assignedRoles),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées de création d'utilisateur", e);
        }
    }

    /**
     * Enregistre une action d'attribution de rôle.
     */
    @Transactional
    public void logRoleGrant(User adminUser, User targetUser, UserRole grantedRole,
                             Set<UserRole> previousRoles, Set<UserRole> currentRoles,
                             String reason, RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("targetUserId", targetUser.getId());
            metadata.put("targetUsername", targetUser.getUsername());
            metadata.put("grantedRole", grantedRole);
            metadata.put("previousRoles", previousRoles);
            metadata.put("currentRoles", currentRoles);
            metadata.put("reason", reason);
            metadata.put("isAdministrativeRole", isAdministrativeRole(grantedRole));
            metadata.put("isSuperAdminRole", grantedRole == UserRole.SUPER_ADMIN);

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ROLE_GRANTED,
                    true, // successful
                    String.format("Attribution du rôle '%s' à l'utilisateur '%s'",
                            grantedRole, targetUser.getUsername()),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées d'attribution de rôle", e);
        }
    }

    /**
     * Enregistre une action de révocation de rôle.
     */
    @Transactional
    public void logRoleRevoke(User adminUser, User targetUser, UserRole revokedRole,
                              Set<UserRole> previousRoles, Set<UserRole> currentRoles,
                              String reason, RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("targetUserId", targetUser.getId());
            metadata.put("targetUsername", targetUser.getUsername());
            metadata.put("revokedRole", revokedRole);
            metadata.put("previousRoles", previousRoles);
            metadata.put("currentRoles", currentRoles);
            metadata.put("reason", reason);
            metadata.put("isAdministrativeRole", isAdministrativeRole(revokedRole));
            metadata.put("isSuperAdminRole", revokedRole == UserRole.SUPER_ADMIN);
            metadata.put("losesAllAdminRoles", !currentRoles.stream().anyMatch(this::isAdministrativeRole));

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ROLE_REVOKED,
                    true, // successful
                    String.format("Révocation du rôle '%s' de l'utilisateur '%s'",
                            revokedRole, targetUser.getUsername()),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées de révocation de rôle", e);
        }
    }

    /**
     * Enregistre une action d'activation d'utilisateur par un admin.
     */
    @Transactional
    public void logUserActivation(User adminUser, User targetUser, boolean wasPreviouslyDeactivated,
                                  RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("targetUserId", targetUser.getId());
            metadata.put("targetUsername", targetUser.getUsername());
            metadata.put("wasPreviouslyDeactivated", wasPreviouslyDeactivated);
            metadata.put("isAdministrativeUser", isAdministrativeUser(targetUser));
            metadata.put("activationType", wasPreviouslyDeactivated ? "REACTIVATION" : "ACTIVATION");

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ACCOUNT_ACTIVATED,
                    true, // successful
                    String.format("Activation de l'utilisateur '%s'", targetUser.getUsername()),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées d'activation d'utilisateur", e);
        }
    }

    /**
     * Enregistre une action de désactivation d'utilisateur par un admin.
     */
    @Transactional
    public void logUserDeactivation(User adminUser, User targetUser, AdminDeactivationCategory category,
                                    String comment, boolean invalidateActiveSessions,
                                    RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("targetUserId", targetUser.getId());
            metadata.put("targetUsername", targetUser.getUsername());
            metadata.put("deactivationCategory", category);
            metadata.put("comment", comment);
            metadata.put("invalidateActiveSessions", invalidateActiveSessions);
            metadata.put("isAdministrativeUser", isAdministrativeUser(targetUser));
            metadata.put("severityLevel", category.getSeverityLevel());
            metadata.put("allowsReactivation", category.allowsReactivation());

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ACCOUNT_DEACTIVATED,
                    true, // successful
                    String.format("Désactivation de l'utilisateur '%s' (Catégorie: %s)",
                            targetUser.getUsername(), category.getDisplayName()),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées de désactivation d'utilisateur", e);
        }
    }

    /**
     * Enregistre une action de reset de mot de passe déclenché par un admin.
     */
    @Transactional
    public void logPasswordResetTriggered(User adminUser, User targetUser, String reason,
                                          boolean forceChangeOnNextLogin, boolean invalidateActiveSessions,
                                          RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("targetUserId", targetUser.getId());
            metadata.put("targetUsername", targetUser.getUsername());
            metadata.put("reason", reason);
            metadata.put("forceChangeOnNextLogin", forceChangeOnNextLogin);
            metadata.put("invalidateActiveSessions", invalidateActiveSessions);
            metadata.put("isAdministrativeUser", isAdministrativeUser(targetUser));
            metadata.put("isSecurityRelated", isSecurityRelatedReason(reason));

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ADMIN_PASSWORD_RESET,
                    true, // successful
                    String.format("Reset de mot de passe déclenché pour l'utilisateur '%s'",
                            targetUser.getUsername()),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées de reset de mot de passe", e);
        }
    }

    /**
     * Enregistre une recherche d'utilisateurs par un admin.
     */
    @Transactional
    public void logUserSearch(User adminUser, String query, long totalResults, long searchDurationMs,
                              RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("searchQuery", query);
            metadata.put("totalResults", totalResults);
            metadata.put("searchDurationMs", searchDurationMs);
            metadata.put("isSlowSearch", searchDurationMs > 5000);

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ADMIN_USER_SEARCH,
                    true, // successful
                    String.format("Recherche d'utilisateurs: '%s' (%d résultats)", query, totalResults),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées de recherche d'utilisateurs", e);
        }
    }

    /**
     * Enregistre une suppression d'utilisateur par un admin.
     */
    @Transactional
    public void logUserDeletion(User adminUser, User targetUser, String reason,
                                boolean isGdprDeletion, RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("targetUserId", targetUser.getId());
            metadata.put("targetUsername", targetUser.getUsername());
            metadata.put("targetEmail", targetUser.getEmail());
            metadata.put("reason", reason);
            metadata.put("isGdprDeletion", isGdprDeletion);
            metadata.put("isAdministrativeUser", isAdministrativeUser(targetUser));
            metadata.put("userRoles", targetUser.getUserRoles());

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ADMIN_USER_DELETION,
                    true, // successful
                    String.format("Suppression de l'utilisateur '%s'%s",
                            targetUser.getUsername(),
                            isGdprDeletion ? " (GDPR)" : ""),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées de suppression d'utilisateur", e);
        }
    }

    /**
     * Enregistre un accès aux logs d'audit par un admin.
     */
    @Transactional
    public void logAuditAccess(User adminUser, String accessType, String targetUserId,
                               String timeRange, RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("accessType", accessType);
            metadata.put("targetUserId", targetUserId);
            metadata.put("timeRange", timeRange);
            metadata.put("accessTimestamp", Instant.now());

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ADMIN_AUDIT_ACCESS,
                    true, // successful
                    String.format("Accès aux logs d'audit (%s)%s",
                            accessType,
                            targetUserId != null ? " pour utilisateur ID " + targetUserId : ""),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées d'accès aux logs d'audit", e);
        }
    }

    /**
     * Enregistre un export de données par un admin.
     */
    @Transactional
    public void logDataExport(User adminUser, String exportType, String criteria,
                              long recordCount, RequestContext requestContext) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exportType", exportType);
            metadata.put("criteria", criteria);
            metadata.put("recordCount", recordCount);
            metadata.put("exportTimestamp", Instant.now());

            logUserAction(
                    adminUser,
                    null, // device
                    ActionLogType.ADMIN_DATA_EXPORT,
                    true, // successful
                    String.format("Export de données (%s): %d enregistrements", exportType, recordCount),
                    objectMapper.writeValueAsString(metadata),
                    requestContext
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées d'export de données", e);
        }
    }

    /**
     * Enregistre une action administrative générique.
     */
    @Transactional
    public void logAdminAction(AdminActionEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("actionType", event.actionType());
            metadata.put("result", event.result());
            metadata.put("resultMessage", event.resultMessage());
            metadata.put("targetUserId", event.getTargetUserId());
            metadata.put("targetUsername", event.getTargetUsername());
            metadata.put("isSuccess", event.isSuccess());
            metadata.put("isSecurityAction", event.isSecurityAction());
            metadata.putAll(event.actionDetails());

            ActionLogType logType = mapActionTypeToLogType(event.actionType());

            logUserAction(
                    event.adminUser(),
                    null, // device
                    logType,
                    event.isSuccess(), // successful
                    event.actionDescription(),
                    objectMapper.writeValueAsString(metadata),
                    event.requestContext()
            );
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation des métadonnées d'action administrative", e);
        }
    }

    // ================== MÉTHODES DE RECHERCHE SPÉCIALISÉES ==================

    // ================== MÉTHODES DE RECHERCHE TYPE SAFE ==================

    /**
     * Recherche les actions d'administration par type d'action (TYPE SAFE).
     */
    public Page<ActivityLog> getAdminActionsByType(List<ActionLogType> actionTypes, Pageable pageable) {
        return activityLogRepository.findByActionTypeInOrderByTimestampDesc(actionTypes, pageable);
    }

    /**
     * Recherche les actions d'administration effectuées par un administrateur spécifique (TYPE SAFE).
     */
    public Page<ActivityLog> getAdminActionsByUser(User adminUser, Instant from, Instant to, Pageable pageable) {
        List<ActionLogType> adminActionTypes = getAdminActionTypes();
        return activityLogRepository.findByUserAndActionTypeInAndTimestampBetween(
                adminUser, adminActionTypes, from, to, pageable);
    }

    /**
     * Recherche les actions sensibles (sécurité, rôles administratifs, etc.) - TYPE SAFE.
     */
    public Page<ActivityLog> getSensitiveAdminActions(Instant from, Instant to, Pageable pageable) {
        List<ActionLogType> sensitiveActions = getSensitiveActionTypes();
        return activityLogRepository.findByActionTypeInAndTimestampBetweenOrderByTimestampDesc(
                sensitiveActions, from, to, pageable);
    }

    /**
     * Recherche les échecs d'actions administratives (TYPE SAFE).
     */
    public Page<ActivityLog> getFailedAdminActions(Instant from, Instant to, Pageable pageable) {
        List<ActionLogType> adminActions = getAdminActionTypes();

        return activityLogRepository.searchLogsWithEnums(
                null, // userId
                null, // ipAddress
                adminActions,
                false, // successful = false pour les échecs
                from,
                to,
                pageable
        );
    }

    /**
     * Recherche les actions d'administration par utilisateur et types spécifiques (TYPE SAFE).
     */
    public Page<ActivityLog> getAdminActionsByUserAndTypes(User adminUser, List<ActionLogType> actionTypes,
                                                           Instant from, Instant to, Pageable pageable) {
        return activityLogRepository.findByUserAndActionTypeInAndTimestampBetween(
                adminUser, actionTypes, from, to, pageable);
    }

    /**
     * Obtient les statistiques d'actions d'administration pour une période (TYPE SAFE).
     */
    public Map<ActionLogType, Long> getAdminActionStatistics(Instant from, Instant to) {
        Map<ActionLogType, Long> stats = new HashMap<>();

        List<ActionLogType> adminActions = getAdminActionTypes();

        for (ActionLogType actionType : adminActions) {
            long count = activityLogRepository.countByActionTypeAndSuccessfulAndTimestampBetween(
                    actionType, true, from, to);
            stats.put(actionType, count); // ← Clé en ActionLogType, pas en String !
        }

        return stats;
    }

    /**
     * Recherche flexible par critères multiples (TYPE SAFE).
     */
    public Page<ActivityLog> searchAdminLogs(Long userId, String ipAddress, List<ActionLogType> actionTypes,
                                             Boolean successful, Instant from, Instant to, Pageable pageable) {
        return activityLogRepository.searchLogsWithEnums(
                userId, ipAddress, actionTypes, successful, from, to, pageable);
    }

    /**
     * Obtient les dernières actions d'un type spécifique par un admin (TYPE SAFE).
     */
    public List<ActivityLog> getRecentAdminActionsByType(User adminUser, ActionLogType actionType, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return activityLogRepository.findByUserAndActionTypeOrderByTimestampDesc(adminUser, actionType, pageable)
                .getContent();
    }

    /**
     * Obtient les statistiques globales par catégorie d'actions (TYPE SAFE).
     */
    public Map<String, Long> getActionCategoryStatistics(Instant from, Instant to) {
        List<Object[]> results = activityLogRepository.getActionTypeStatistics(from, to);

        Map<String, Long> categoryStats = new HashMap<>();

        for (Object[] result : results) {
            ActionLogType actionType = (ActionLogType) result[0];
            Long count = (Long) result[1];

            String category = actionType.getCategory();
            categoryStats.merge(category, count, Long::sum);
        }

        return categoryStats;
    }

    /**
     * Recherche les actions par catégorie (TYPE SAFE).
     */
    public Page<ActivityLog> getActionsByCategory(String category, Instant from, Instant to, Pageable pageable) {
        return activityLogRepository.findByActionCategoryAndTimestampBetween(category, from, to, pageable);
    }

    // ================== MÉTHODES UTILITAIRES PRIVÉES ==================

    /**
     * Définit tous les types d'actions considérés comme "administratives".
     */
    private List<ActionLogType> getAdminActionTypes() {
        return List.of(
                ActionLogType.ADMIN_USER_CREATED,
                ActionLogType.ADMIN_USER_DELETION,
                ActionLogType.ADMIN_USER_SEARCH,
                ActionLogType.ADMIN_PASSWORD_RESET,
                ActionLogType.ADMIN_FORCE_LOGOUT,
                ActionLogType.ADMIN_DATA_EXPORT,
                ActionLogType.ADMIN_AUDIT_ACCESS,
                ActionLogType.ROLE_GRANTED,
                ActionLogType.ROLE_REVOKED,
                ActionLogType.ACCOUNT_ACTIVATED,
                ActionLogType.ACCOUNT_DEACTIVATED
        );
    }

    /**
     * Définit tous les types d'actions considérés comme "sensibles".
     */
    private List<ActionLogType> getSensitiveActionTypes() {
        return List.of(
                ActionLogType.ROLE_GRANTED,
                ActionLogType.ROLE_REVOKED,
                ActionLogType.ACCOUNT_DEACTIVATED,
                ActionLogType.ADMIN_PASSWORD_RESET,
                ActionLogType.ADMIN_USER_CREATED,
                ActionLogType.ADMIN_USER_DELETION,
                ActionLogType.ADMIN_USER_SEARCH,
                ActionLogType.ADMIN_AUDIT_ACCESS,
                ActionLogType.SECURITY_SUSPICIOUS_ACTIVITY
        );
    }

    // ================== MÉTHODES UTILITAIRES PRIVÉES ==================

    /**
     * Vérifie si un rôle est administratif.
     */
    private boolean isAdministrativeRole(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.MODERATOR;
    }

    /**
     * Vérifie si un utilisateur a des rôles administratifs.
     */
    private boolean isAdministrativeUser(User user) {
        return user.getUserRoles().stream().anyMatch(this::isAdministrativeRole);
    }

    /**
     * Vérifie si une raison est liée à la sécurité.
     */
    private boolean isSecurityRelatedReason(String reason) {
        if (reason == null) return false;
        String lowerReason = reason.toLowerCase();
        return lowerReason.contains("sécurité") || lowerReason.contains("security") ||
                lowerReason.contains("compromis") || lowerReason.contains("breach");
    }

    /**
     * Mappe un type d'action admin vers un type de log d'activité.
     */
    private ActionLogType mapActionTypeToLogType(AdminActionEvent.AdminActionType actionType) {
        return switch (actionType) {
            case USER_CREATION -> ActionLogType.ADMIN_USER_CREATED;
            case USER_DELETION -> ActionLogType.ADMIN_USER_DELETION;
            case USER_ACTIVATION -> ActionLogType.ACCOUNT_ACTIVATED;
            case USER_DEACTIVATION -> ActionLogType.ACCOUNT_DEACTIVATED;
            case ROLE_GRANT -> ActionLogType.ROLE_GRANTED;
            case ROLE_REVOKE -> ActionLogType.ROLE_REVOKED;
            case PASSWORD_RESET -> ActionLogType.ADMIN_PASSWORD_RESET;
            case USER_SEARCH -> ActionLogType.ADMIN_USER_SEARCH;
            case DATA_EXPORT -> ActionLogType.ADMIN_DATA_EXPORT;
            case SYSTEM_CONFIGURATION -> ActionLogType.SYSTEM_CONFIG_CHANGED;
            case SECURITY_ACTION -> ActionLogType.SECURITY_SUSPICIOUS_ACTIVITY;
            case AUDIT_ACCESS -> ActionLogType.ADMIN_AUDIT_ACCESS;
            default -> ActionLogType.API_ACCESS;
        };
    }
}