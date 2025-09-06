package be.steby.CoreProject.bll.domains.password.logs;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.activitylog.AbstractActivityLogService;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.oldActionLogType;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de journalisation spécifique aux opérations liées aux mots de passe.
 * Cette classe s'occupe de l'enregistrement et l'analyse des activités
 * telles que les changements de mot de passe, les demandes de réinitialisation, etc.
 */
@Service
@Slf4j
public class PasswordActivityLogService extends AbstractActivityLogService {

    /**
     * Constructeur pour l'initialisation des dépendances.
     *
     * @param activityLogRepository Repository pour la persistance des logs d'activité
     */
    public PasswordActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    /**
     * Retourne le nom du domaine pour ce service.
     *
     * @return Le nom du domaine "PASSWORD"
     */
    @Override
    protected String getDomainName() {
        return "PASSWORD";
    }

    @Override
    protected int getBaseRiskForActionType(oldActionLogType actionType) {
        return 0;
    }

    /**
     * Enregistre un changement de mot de passe
     *
     * @param user L'utilisateur qui a changé son mot de passe
     * @param device L'appareil utilisé
     * @param successful Si le changement a réussi
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logPasswordChange(
            User user,
            Device device,
            boolean successful,
            RequestContext requestContext) {

        String details = successful
                ? "Changement de mot de passe réussi"
                : "Échec du changement de mot de passe";

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("isMustChangePasswordReset", user.isMustChangePassword());
            metadata.put("source", "user_initiated");
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour le changement de mot de passe: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                oldActionLogType.PASSWORD_CHANGED,
                successful,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre une demande de réinitialisation de mot de passe
     *
     * @param user L'utilisateur concerné par la demande
     * @param device L'appareil utilisé
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logPasswordResetRequest(
            User user,
            Device device,
            RequestContext requestContext) {

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("requestTime", Instant.now().toString());
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la demande de réinitialisation: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                oldActionLogType.PASSWORD_RESET_REQUEST,
                true,
                "Demande de réinitialisation de mot de passe effectuée",
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre un renouvellement de demande de réinitialisation de mot de passe
     * (quand un token a expiré et qu'un nouveau token est demandé)
     *
     * @param user L'utilisateur concerné par la demande
     * @param device L'appareil utilisé
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logRequestPasswordToken(
            User user,
            Device device,
            RequestContext requestContext) {

        return logUserAction(
                user,
                device,
                oldActionLogType.PASSWORD_REQUEST_TOKEN,
                true,
                "Nouvelle demande de token de réinitialisation de mot de passe",
                null,
                requestContext
        );
    }

    /**
     * Enregistre une réinitialisation complète de mot de passe
     *
     * @param user L'utilisateur dont le mot de passe a été réinitialisé
     * @param device L'appareil utilisé
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logPasswordResetComplete(
            User user,
            Device device,
            RequestContext requestContext) {

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("completionTime", Instant.now().toString());
            if (user.isMustChangePassword()) {
                metadata.put("mustChangePassword", "reset");
            }
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la réinitialisation: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                oldActionLogType.PASSWORD_RESET_COMPLETE,
                true,
                "Réinitialisation de mot de passe effectuée",
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre l'expiration d'un mot de passe
     *
     * @param user L'utilisateur dont le mot de passe a expiré
     * @param device L'appareil utilisé
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logPasswordExpired(
            User user,
            Device device,
            RequestContext requestContext) {

        return logUserAction(
                user,
                device,
                oldActionLogType.PASSWORD_EXPIRED,
                true,
                "Mot de passe expiré",
                null,
                requestContext
        );
    }

    /**
     * Enregistre un changement de mot de passe initié par un administrateur
     *
     * @param user L'utilisateur dont le mot de passe a été changé
     * @param adminId L'ID de l'administrateur qui a initié l'action
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAdminPasswordReset(
            User user,
            Long adminId,
            RequestContext requestContext) {

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("adminId", adminId);
            metadata.put("source", "admin_initiated");
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la réinitialisation admin: {}", e.getMessage());
        }

        return logUserAction(
                user,
                null, // Pas d'appareil associé pour une action admin
                oldActionLogType.ADMIN_PASSWORD_RESET,
                true,
                "Réinitialisation de mot de passe par l'administrateur #" + adminId,
                metadataJson,
                requestContext
        );
    }

    /**
     * Obtient l'historique des activités liées aux mots de passe d'un utilisateur
     *
     * @param user L'utilisateur dont on veut l'historique
     * @param days Nombre de jours d'historique à récupérer
     * @return Une liste d'activités liées aux mots de passe
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getPasswordActivityHistory(User user, int days) {
        Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant endDate = Instant.now();

        return activityLogRepository.findByUserAndActionTypeInAndTimestampBetween(
                user,
                List.of(
                        oldActionLogType.PASSWORD_CHANGED,
                        oldActionLogType.PASSWORD_RESET_REQUEST,
                        oldActionLogType.PASSWORD_RESET_COMPLETE,
                        oldActionLogType.PASSWORD_EXPIRED,
                        oldActionLogType.ADMIN_PASSWORD_RESET
                ),
                startDate,
                endDate,
                null
        ).getContent();
    }

    /**
     * Calcule le nombre de jours depuis le dernier changement de mot de passe
     *
     * @param user L'utilisateur pour lequel effectuer le calcul
     * @return Le nombre de jours depuis le dernier changement ou -1 si aucun changement trouvé
     */
    @Transactional(readOnly = true)
    public int daysSinceLastPasswordChange(User user) {
        ActivityLog lastPasswordChange = activityLogRepository.findTopByUserAndActionTypeAndSuccessfulOrderByTimestampDesc(
                user, oldActionLogType.PASSWORD_CHANGED.name(), true);

        if (lastPasswordChange == null) {
            return -1;
        }

        Instant now = Instant.now();
        Instant lastChange = lastPasswordChange.getTimestamp();

        return (int) ChronoUnit.DAYS.between(lastChange, now);
    }

    /**
     * Vérifie si un utilisateur a atteint le nombre maximum de demandes de réinitialisation
     * dans un intervalle de temps donné
     *
     * @param user L'utilisateur à vérifier
     * @param maxAttempts Le nombre maximum de tentatives autorisées
     * @param timeframeMinutes L'intervalle de temps en minutes
     * @return true si l'utilisateur a dépassé le seuil, false sinon
     */
    @Transactional(readOnly = true)
    public boolean hasExceededResetRequestLimit(User user, int maxAttempts, int timeframeMinutes) {
        Instant startTime = Instant.now().minus(timeframeMinutes, ChronoUnit.MINUTES);

        long count = activityLogRepository.countByUserAndActionTypeAndTimestampBetween(
                user,
                oldActionLogType.PASSWORD_RESET_REQUEST.name(),
                startTime,
                Instant.now()
        );

        return count >= maxAttempts;
    }
}