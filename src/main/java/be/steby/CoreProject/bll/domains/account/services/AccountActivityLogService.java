package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de journalisation spécifique aux opérations liées aux comptes utilisateur.
 * Cette classe s'occupe de l'enregistrement et l'analyse des activités
 * telles que les créations de compte, activations, désactivations, demandes, etc.
 *
 * Ce service complète l'ActivityLogService général en proposant des méthodes
 * spécialisées pour les activités de compte avec une meilleure granularité.
 */
@Service
@Slf4j
public class AccountActivityLogService extends AbstractActivityLogService {

    /**
     * Constructeur pour l'initialisation des dépendances.
     *
     * @param activityLogRepository Repository pour la persistance des logs d'activité
     */
    public AccountActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    /**
     * Retourne le nom du domaine pour ce service.
     *
     * @return Le nom du domaine "ACCOUNT"
     */
    @Override
    protected String getDomainName() {
        return "ACCOUNT";
    }

    // ================== MÉTHODES DE JOURNALISATION SPÉCIALISÉES ==================

    /**
     * Enregistre une demande d'activation de compte
     * Cette méthode trace la demande elle-même, pas la confirmation
     *
     * @param user L'utilisateur qui demande l'activation
     * @param device L'appareil utilisé
     * @param tokenGenerated Si un token d'activation a été généré
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountActivationRequest(
            User user,
            Device device,
            boolean tokenGenerated,
            RequestContext requestContext) {

        String details = tokenGenerated
                ? "Demande d'activation de compte - Token généré et envoyé par email"
                : "Demande d'activation de compte - Échec de génération du token";

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tokenGenerated", tokenGenerated);
            metadata.put("requestTime", Instant.now().toString());
            metadata.put("userEnabled", user.isEnabled());
            metadata.put("emailVerified", user.isEmailVerified());
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la demande d'activation: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_ACTIVATION_REQUESTED,
                tokenGenerated,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre une demande de désactivation de compte
     * Cette méthode trace la demande elle-même, pas la confirmation
     *
     * @param user L'utilisateur qui demande la désactivation
     * @param device L'appareil utilisé
     * @param deactivationReason La raison de désactivation
     * @param reasonDetails Les détails de la raison
     * @param tokenGenerated Si un token de confirmation a été généré
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountDeactivationRequest(
            User user,
            Device device,
            DeactivationReason deactivationReason,
            String reasonDetails,
            boolean tokenGenerated,
            RequestContext requestContext) {

        String details = String.format(
                "Demande de désactivation de compte - Raison: %s%s",
                deactivationReason.name(),
                tokenGenerated ? " - Token de confirmation envoyé" : " - Échec d'envoi du token"
        );

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("deactivationReason", deactivationReason.name());
            metadata.put("reasonDetails", reasonDetails);
            metadata.put("tokenGenerated", tokenGenerated);
            metadata.put("requestTime", Instant.now().toString());
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la demande de désactivation: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_DEACTIVATION_REQUESTED,
                tokenGenerated,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre une tentative de confirmation d'activation échouée
     * Utile pour tracer les tentatives avec des tokens invalides ou expirés
     *
     * @param user L'utilisateur concerné
     * @param device L'appareil utilisé
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token qui a été utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountActivationAttemptFailed(
            User user,
            Device device,
            String failureReason,
            String tokenUsed,
            RequestContext requestContext) {

        String details = "Échec de confirmation d'activation - " + failureReason;

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("failureReason", failureReason);
            metadata.put("tokenPrefix", tokenUsed != null && tokenUsed.length() > 8
                    ? tokenUsed.substring(0, 8) + "..." : "unknown");
            metadata.put("attemptTime", Instant.now().toString());
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la tentative d'activation échouée: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_ACTIVATION_ATTEMPT_FAILED,
                false,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre une tentative de désactivation échouée
     *
     * @param user L'utilisateur concerné
     * @param device L'appareil utilisé
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token qui a été utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountDeactivationAttemptFailed(
            User user,
            Device device,
            String failureReason,
            String tokenUsed,
            RequestContext requestContext) {

        String details = "Échec de confirmation de désactivation - " + failureReason;

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("failureReason", failureReason);
            metadata.put("tokenPrefix", tokenUsed != null && tokenUsed.length() > 8
                    ? tokenUsed.substring(0, 8) + "..." : "unknown");
            metadata.put("attemptTime", Instant.now().toString());
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la tentative de désactivation échouée: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_DEACTIVATION_ATTEMPT_FAILED,
                false,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre la révocation d'un token d'activation ou de désactivation
     *
     * @param user L'utilisateur concerné
     * @param device L'appareil utilisé
     * @param tokenType Le type de token révoqué
     * @param reason La raison de révocation
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountTokenRevocation(
            User user,
            Device device,
            String tokenType,
            String reason,
            RequestContext requestContext) {

        String details = String.format("Révocation de token %s - Raison: %s", tokenType, reason);

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tokenType", tokenType);
            metadata.put("revocationReason", reason);
            metadata.put("revocationTime", Instant.now().toString());
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la révocation de token: {}", e.getMessage());
        }

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_TOKEN_REVOKED,
                true,
                details,
                metadataJson,
                requestContext
        );
    }

    // ================== MÉTHODES D'ANALYSE SPÉCIALISÉES ==================

    /**
     * Récupère l'historique complet des activités de compte pour un utilisateur
     *
     * @param user L'utilisateur concerné
     * @param pageable Configuration de pagination
     * @return Page d'activités de compte
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAccountActivityHistory(User user, Pageable pageable) {
        List<String> accountActionTypes = List.of(
                ActionLogType.ACCOUNT_CREATED.name(),
                ActionLogType.ACCOUNT_ACTIVATED.name(),
                ActionLogType.ACCOUNT_DEACTIVATED.name(),
                ActionLogType.ACCOUNT_ACTIVATION_REQUESTED.name(),
                ActionLogType.ACCOUNT_DEACTIVATION_REQUESTED.name(),
                ActionLogType.ACCOUNT_ACTIVATION_ATTEMPT_FAILED.name(),
                ActionLogType.ACCOUNT_DEACTIVATION_ATTEMPT_FAILED.name(),
                ActionLogType.ACCOUNT_TOKEN_REVOKED.name()
        );

        return activityLogRepository.findByUserAndActionTypeInOrderByTimestampDesc(
                user, accountActionTypes, pageable);
    }

    /**
     * Compte les tentatives d'activation échouées pour un utilisateur dans une période donnée
     *
     * @param user L'utilisateur concerné
     * @param hours Nombre d'heures à examiner
     * @return Nombre de tentatives échouées
     */
    @Transactional(readOnly = true)
    public long countFailedActivationAttempts(User user, int hours) {
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        return activityLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampAfter(
                user,
                ActionLogType.ACCOUNT_ACTIVATION_ATTEMPT_FAILED.name(),
                false,
                cutoff
        );
    }

    /**
     * Compte les tentatives de désactivation échouées pour un utilisateur dans une période donnée
     *
     * @param user L'utilisateur concerné
     * @param hours Nombre d'heures à examiner
     * @return Nombre de tentatives échouées
     */
    @Transactional(readOnly = true)
    public long countFailedDeactivationAttempts(User user, int hours) {
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        return activityLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampAfter(
                user,
                ActionLogType.ACCOUNT_DEACTIVATION_ATTEMPT_FAILED.name(),
                false,
                cutoff
        );
    }

    /**
     * Détecte une activité suspecte liée aux comptes
     * Par exemple : multiples tentatives d'activation/désactivation échouées
     *
     * @param user L'utilisateur à analyser
     * @return true si une activité suspecte est détectée
     */
    @Transactional(readOnly = true)
    public boolean detectSuspiciousAccountActivity(User user) {
        // Vérifier les tentatives échouées dans les dernières 24 heures
        long failedActivationAttempts = countFailedActivationAttempts(user, 24);
        long failedDeactivationAttempts = countFailedDeactivationAttempts(user, 24);

        // Seuil de suspicion : plus de 5 tentatives échouées
        boolean suspicious = (failedActivationAttempts + failedDeactivationAttempts) > 5;

        if (suspicious) {
            log.warn("Activité suspecte détectée pour l'utilisateur {} - {} tentatives d'activation échouées, {} tentatives de désactivation échouées",
                    user.getUsername(), failedActivationAttempts, failedDeactivationAttempts);
        }

        return suspicious;
    }

    /**
     * Récupère les dernières demandes de compte (activation/désactivation) pour un utilisateur
     *
     * @param user L'utilisateur concerné
     * @param limit Nombre maximum de résultats
     * @return Liste des dernières demandes
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentAccountRequests(User user, int limit) {
        List<String> requestActionTypes = List.of(
                ActionLogType.ACCOUNT_ACTIVATION_REQUESTED.name(),
                ActionLogType.ACCOUNT_DEACTIVATION_REQUESTED.name()
        );

        // Utilise Pageable pour limiter les résultats
        Pageable pageable = Pageable.ofSize(limit);
        return activityLogRepository.findTopNByUserAndActionTypeInOrderByTimestampDesc(
                user, requestActionTypes, pageable);
    }
}