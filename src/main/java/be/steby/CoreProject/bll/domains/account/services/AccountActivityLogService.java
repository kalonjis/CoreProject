package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.activitylog.AbstractActivityLogService;
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
 *
 * IMPORTANT: Pour utiliser les méthodes de réactivation, vous devez ajouter
 * ces constantes dans l'enum ActionLogType :
 * - ACCOUNT_REACTIVATION_REQUESTED("Demande de réactivation de compte utilisateur")
 * - ACCOUNT_REACTIVATION_CONFIRMED("Réactivation de compte confirmée")
 * - ACCOUNT_REACTIVATION_ATTEMPT_FAILED("Tentative de réactivation de compte échouée")
 */
@Service
@Slf4j
public class AccountActivityLogService extends AbstractActivityLogService {

    // ================== CONSTRUCTEUR ET MÉTHODES HÉRITÉES ==================

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

    @Override
    protected int getBaseRiskForActionType(ActionLogType actionType) {
        return 0;
    }

    // ================== MÉTHODES UTILITAIRES PRIVÉES ==================

    /**
     * Crée des métadonnées JSON pour les demandes d'activation/désactivation.
     *
     * @param tokenGenerated Si un token a été généré
     * @param user L'utilisateur concerné
     * @param reason Raison de la demande (peut être null)
     * @param reasonDetails Détails de la raison (peut être null)
     * @return JSON string ou null en cas d'erreur
     */
    private String createRequestMetadata(boolean tokenGenerated, User user,
                                         DeactivationReason reason, String reasonDetails) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tokenGenerated", tokenGenerated);
            metadata.put("requestTime", Instant.now().toString());
            metadata.put("userEnabled", user.isEnabled());
            metadata.put("emailVerified", user.isEmailVerified());

            if (reason != null) {
                metadata.put("deactivationReason", reason.name());
                metadata.put("reasonDetails", reasonDetails);
            }

            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la demande: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crée des métadonnées JSON pour les demandes de réactivation.
     *
     * @param tokenGenerated Si un token a été généré
     * @param user L'utilisateur concerné
     * @return JSON string ou null en cas d'erreur
     */
    private String createReactivationRequestMetadata(boolean tokenGenerated, User user) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tokenGenerated", tokenGenerated);
            metadata.put("requestTime", Instant.now().toString());
            metadata.put("userCurrentlyEnabled", user.isEnabled());
            metadata.put("emailVerified", user.isEmailVerified());
            metadata.put("wasEverActivated", user.isEverActivated());
            metadata.put("deactivatedAt", user.getDeactivatedAt() != null ? user.getDeactivatedAt().toString() : null);
            metadata.put("accountDeactivatedDuration", calculateDeactivationDuration(user));

            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la demande de réactivation: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crée des métadonnées JSON pour les tentatives échouées.
     *
     * @param failureReason Raison de l'échec
     * @param tokenUsed Token utilisé (sera tronqué pour sécurité)
     * @param user L'utilisateur concerné
     * @return JSON string ou null en cas d'erreur
     */
    private String createFailureMetadata(String failureReason, String tokenUsed, User user) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("failureReason", failureReason);
            metadata.put("tokenPrefix", tokenUsed != null && tokenUsed.length() > 8
                    ? tokenUsed.substring(0, 8) + "..."
                    : "N/A");
            metadata.put("attemptTime", Instant.now().toString());
            metadata.put("userCurrentState", user.isEnabled() ? "ACTIVE" : "INACTIVE");

            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la tentative échouée: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crée des métadonnées JSON pour les confirmations.
     *
     * @param user L'utilisateur concerné
     * @param reason Raison de la désactivation (peut être null)
     * @param reasonDetails Détails de la raison (peut être null)
     * @return JSON string ou null en cas d'erreur
     */
    private String createConfirmationMetadata(User user, DeactivationReason reason, String reasonDetails) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("confirmationTime", Instant.now().toString());
            metadata.put("userAccountAge", calculateAccountAge(user));
            metadata.put("wasEmailVerified", user.isEmailVerified());
            metadata.put("wasEverActivated", user.isEverActivated());
            metadata.put("lastActiveDate", user.getActivatedAt() != null ? user.getActivatedAt().toString() : null);

            if (reason != null) {
                metadata.put("deactivationReason", reason.name());
                metadata.put("reasonDetails", reasonDetails);
            }

            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la confirmation: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crée des métadonnées JSON pour les confirmations de réactivation.
     *
     * @param user L'utilisateur concerné
     * @return JSON string ou null en cas d'erreur
     */
    private String createReactivationConfirmationMetadata(User user) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("reactivationTime", Instant.now().toString());
            metadata.put("userAccountAge", calculateAccountAge(user));
            metadata.put("emailVerified", user.isEmailVerified());
            metadata.put("wasEverActivated", user.isEverActivated());
            metadata.put("previousDeactivationDate", user.getDeactivatedAt() != null ? user.getDeactivatedAt().toString() : null);
            metadata.put("deactivationDuration", calculateDeactivationDuration(user));
            metadata.put("lastActiveBeforeDeactivation", user.getActivatedAt() != null ? user.getActivatedAt().toString() : null);

            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les métadonnées pour la confirmation de réactivation: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calcule l'âge du compte en jours.
     *
     * @param user L'utilisateur concerné
     * @return Âge du compte en jours
     */
    private long calculateAccountAge(User user) {
        if (user.getCreatedAt() != null) {
            return ChronoUnit.DAYS.between(user.getCreatedAt(), Instant.now());
        }
        return 0;
    }

    /**
     * Calcule la durée de désactivation en jours.
     *
     * @param user L'utilisateur concerné
     * @return Durée de désactivation en jours, ou 0 si le compte n'a jamais été désactivé
     */
    private long calculateDeactivationDuration(User user) {
        if (user.getDeactivatedAt() != null) {
            return ChronoUnit.DAYS.between(user.getDeactivatedAt(), Instant.now());
        }
        return 0;
    }

    // ================== MÉTHODES DE JOURNALISATION - DEMANDES ==================

    /**
     * Enregistre une demande d'activation de compte.
     * Cette méthode trace la demande elle-même, pas la confirmation.
     *
     * @param user L'utilisateur qui demande l'activation
     * @param device L'appareil utilisé
     * @param tokenGenerated Si un token d'activation a été généré
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountActivationRequest(User user, Device device,
                                                   boolean tokenGenerated, RequestContext requestContext) {
        String details = tokenGenerated
                ? "Demande d'activation de compte - Token généré et envoyé par email"
                : "Demande d'activation de compte - Échec de génération du token";

        String metadataJson = createRequestMetadata(tokenGenerated, user, null, null);

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
     * Enregistre une demande de désactivation de compte.
     * Cette méthode trace la demande elle-même, pas la confirmation.
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
    public ActivityLog logAccountDeactivationRequest(User user, Device device,
                                                     DeactivationReason deactivationReason,
                                                     String reasonDetails, boolean tokenGenerated,
                                                     RequestContext requestContext) {
        String details = String.format(
                "Demande de désactivation de compte - Raison: %s%s",
                deactivationReason.name(),
                tokenGenerated ? " - Token de confirmation envoyé" : " - Échec d'envoi du token"
        );

        String metadataJson = createRequestMetadata(tokenGenerated, user, deactivationReason, reasonDetails);

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
     * Enregistre une demande de réactivation de compte.
     * Cette méthode trace la demande elle-même, pas la confirmation.
     *
     * @param user L'utilisateur qui demande la réactivation
     * @param device L'appareil utilisé
     * @param tokenGenerated Si un token de réactivation a été généré
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountReactivationRequest(User user, Device device,
                                                     boolean tokenGenerated, RequestContext requestContext) {
        String details = tokenGenerated
                ? "Demande de réactivation de compte - Token généré et envoyé par email"
                : "Demande de réactivation de compte - Échec de génération du token";

        String metadataJson = createReactivationRequestMetadata(tokenGenerated, user);

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_REACTIVATION_REQUESTED,
                tokenGenerated,
                details,
                metadataJson,
                requestContext
        );
    }

    // ================== MÉTHODES DE JOURNALISATION - CONFIRMATIONS ==================

    /**
     * Enregistre la confirmation d'une activation de compte.
     * Cette méthode trace l'activation effective après confirmation du token.
     *
     * @param user L'utilisateur qui a été activé
     * @param device L'appareil utilisé pour la confirmation
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountActivationConfirmed(User user, Device device, RequestContext requestContext) {
        String details = "Activation de compte confirmée avec succès";
        String metadataJson = createConfirmationMetadata(user, null, null);

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_ACTIVATED,
                true,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre la confirmation d'une désactivation de compte.
     * Cette méthode trace la désactivation effective après confirmation du token.
     *
     * @param user L'utilisateur qui a été désactivé
     * @param device L'appareil utilisé pour la confirmation
     * @param deactivationReason La raison de désactivation
     * @param reasonDetails Les détails de la raison
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountDeactivationConfirmed(User user, Device device,
                                                       DeactivationReason deactivationReason,
                                                       String reasonDetails, RequestContext requestContext) {
        String details = String.format(
                "Désactivation de compte confirmée - Raison: %s%s",
                deactivationReason.name(),
                reasonDetails != null && !reasonDetails.trim().isEmpty()
                        ? " - Détails: " + reasonDetails.trim()
                        : ""
        );

        String metadataJson = createConfirmationMetadata(user, deactivationReason, reasonDetails);

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_DEACTIVATED,
                true,
                details,
                metadataJson,
                requestContext
        );
    }

    /**
     * Enregistre la confirmation d'une réactivation de compte.
     * Cette méthode trace la réactivation effective après confirmation du token.
     *
     * @param user L'utilisateur qui a été réactivé
     * @param device L'appareil utilisé pour la confirmation
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountReactivationConfirmed(User user, Device device, RequestContext requestContext) {
        String details = "Réactivation de compte confirmée avec succès";
        String metadataJson = createReactivationConfirmationMetadata(user);

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_REACTIVATION_COMPLETED,
                true,
                details,
                metadataJson,
                requestContext
        );
    }

    // ================== MÉTHODES DE JOURNALISATION - ÉCHECS ==================

    /**
     * Enregistre une tentative de confirmation d'activation échouée.
     * Utile pour tracer les tentatives avec des tokens invalides ou expirés.
     *
     * @param user L'utilisateur concerné
     * @param device L'appareil utilisé
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token qui a été utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountActivationAttemptFailed(User user, Device device,
                                                         String failureReason, String tokenUsed,
                                                         RequestContext requestContext) {
        String details = "Échec de confirmation d'activation - " + failureReason;
        String metadataJson = createFailureMetadata(failureReason, tokenUsed, user);

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
     * Enregistre une tentative de confirmation de désactivation échouée.
     * Utile pour tracer les tentatives avec des tokens invalides ou expirés.
     *
     * @param user L'utilisateur concerné
     * @param device L'appareil utilisé
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token qui a été utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountDeactivationAttemptFailed(User user, Device device,
                                                           String failureReason, String tokenUsed,
                                                           RequestContext requestContext) {
        String details = "Échec de confirmation de désactivation - " + failureReason;
        String metadataJson = createFailureMetadata(failureReason, tokenUsed, user);

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
     * Enregistre une tentative de confirmation de réactivation échouée.
     * Utile pour tracer les tentatives avec des tokens invalides ou expirés.
     *
     * @param user L'utilisateur concerné
     * @param device L'appareil utilisé
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token qui a été utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountReactivationAttemptFailed(User user, Device device,
                                                           String failureReason, String tokenUsed,
                                                           RequestContext requestContext) {
        String details = "Échec de confirmation de réactivation - " + failureReason;
        String metadataJson = createFailureMetadata(failureReason, tokenUsed, user);

        return logUserAction(
                user,
                device,
                ActionLogType.ACCOUNT_REACTIVATION_ATTEMPT_FAILED,
                false,
                details,
                metadataJson,
                requestContext
        );
    }

    // ================== MÉTHODES DE JOURNALISATION - AUTRES ==================

    /**
     * Enregistre la révocation d'un token d'activation ou de désactivation.
     *
     * @param user L'utilisateur concerné
     * @param device L'appareil utilisé
     * @param tokenType Le type de token révoqué
     * @param revocationReason La raison de révocation
     * @param requestContext Le contexte de la requête
     * @return L'entrée de journal créée
     */
    @Transactional
    public ActivityLog logAccountTokenRevocation(User user, Device device,
                                                 String tokenType, String revocationReason,
                                                 RequestContext requestContext) {
        String details = String.format("Révocation de token %s - Raison: %s", tokenType, revocationReason);

        String metadataJson = null;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tokenType", tokenType);
            metadata.put("revocationReason", revocationReason);
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

    // ================== MÉTHODES D'ANALYSE ET DE RÉCUPÉRATION ==================

    /**
     * Récupère l'historique complet des activités de compte pour un utilisateur.
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
                ActionLogType.ACCOUNT_REACTIVATION_REQUESTED.name(),
                ActionLogType.ACCOUNT_ACTIVATION_ATTEMPT_FAILED.name(),
                ActionLogType.ACCOUNT_DEACTIVATION_ATTEMPT_FAILED.name(),
                ActionLogType.ACCOUNT_REACTIVATION_ATTEMPT_FAILED.name(),
                ActionLogType.ACCOUNT_REACTIVATION_COMPLETED.name(),
                ActionLogType.ACCOUNT_TOKEN_REVOKED.name()
        );

        return activityLogRepository.findByUserAndActionTypeInOrderByTimestampDesc(
                user, accountActionTypes, pageable);
    }

    /**
     * Récupère les dernières demandes de compte (activation/désactivation/réactivation) pour un utilisateur.
     *
     * @param user L'utilisateur concerné
     * @param limit Nombre maximum de résultats
     * @return Liste des dernières demandes
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentAccountRequests(User user, int limit) {
        List<String> requestActionTypes = List.of(
                ActionLogType.ACCOUNT_ACTIVATION_REQUESTED.name(),
                ActionLogType.ACCOUNT_DEACTIVATION_REQUESTED.name(),
                ActionLogType.ACCOUNT_REACTIVATION_REQUESTED.name()
        );

        Pageable pageable = Pageable.ofSize(limit);
        return activityLogRepository.findTopNByUserAndActionTypeInOrderByTimestampDesc(
                user, requestActionTypes, pageable);
    }

    // ================== MÉTHODES D'ANALYSE DE SÉCURITÉ ==================

    /**
     * Compte les tentatives d'activation échouées pour un utilisateur dans une période donnée.
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
     * Compte les tentatives de désactivation échouées pour un utilisateur dans une période donnée.
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
     * Compte les tentatives de réactivation échouées pour un utilisateur dans une période donnée.
     *
     * @param user L'utilisateur concerné
     * @param hours Nombre d'heures à examiner
     * @return Nombre de tentatives échouées
     */
    @Transactional(readOnly = true)
    public long countFailedReactivationAttempts(User user, int hours) {
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        return activityLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampAfter(
                user,
                ActionLogType.ACCOUNT_REACTIVATION_ATTEMPT_FAILED.name(),
                false,
                cutoff
        );
    }

    /**
     * Détecte une activité suspecte liée aux comptes.
     * Par exemple : multiples tentatives d'activation/désactivation/réactivation échouées.
     *
     * @param user L'utilisateur à analyser
     * @return true si une activité suspecte est détectée
     */
    @Transactional(readOnly = true)
    public boolean detectSuspiciousAccountActivity(User user) {
        // Vérifier les tentatives échouées dans les dernières 24 heures
        long failedActivationAttempts = countFailedActivationAttempts(user, 24);
        long failedDeactivationAttempts = countFailedDeactivationAttempts(user, 24);
        long failedReactivationAttempts = countFailedReactivationAttempts(user, 24);

        long totalFailedAttempts = failedActivationAttempts + failedDeactivationAttempts + failedReactivationAttempts;

        // Seuil de suspicion : plus de 5 tentatives échouées au total
        boolean suspicious = totalFailedAttempts > 5;

        if (suspicious) {
            log.warn("Activité suspecte détectée pour l'utilisateur {} - {} tentatives d'activation échouées, {} tentatives de désactivation échouées, {} tentatives de réactivation échouées",
                    user.getUsername(), failedActivationAttempts, failedDeactivationAttempts, failedReactivationAttempts);
        }

        return suspicious;
    }
}