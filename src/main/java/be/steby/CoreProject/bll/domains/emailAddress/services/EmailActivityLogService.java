//package be.steby.CoreProject.bll.domains.emailAddress.services;
//
//import be.steby.CoreProject.bll.common.models.RequestContext;
//import be.steby.CoreProject.bll.common.services.activitylog.AbstractActivityLogService;
//import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
//import be.steby.CoreProject.dl.entities.ActivityLog;
//import be.steby.CoreProject.dl.entities.Device;
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.enums.oldActionLogType;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Service de journalisation spécifique aux opérations liées aux adresses email.
// * Cette classe s'occupe de l'enregistrement et l'analyse des activités
// * telles que les changements d'adresse email, les vérifications, etc.
// */
//@Service
//@Slf4j
//public class EmailActivityLogService extends AbstractActivityLogService {
//
//    /**
//     * Constructeur pour l'initialisation des dépendances.
//     *
//     * @param activityLogRepository Repository pour la persistance des logs d'activité
//     */
//    public EmailActivityLogService(ActivityLogRepository activityLogRepository) {
//        super(activityLogRepository);
//    }
//
//    /**
//     * Retourne le nom du domaine pour ce service.
//     *
//     * @return Le nom du domaine "EMAIL"
//     */
//    @Override
//    protected String getDomainName() {
//        return "EMAIL";
//    }
//
//    @Override
//    protected int getBaseRiskForActionType(oldActionLogType actionType) {
//        return 0;
//    }
//
//    /**
//     * Enregistre une demande de changement d'adresse email
//     *
//     * @param user L'utilisateur qui a demandé le changement
//     * @param device L'appareil utilisé
//     * @param newEmail La nouvelle adresse email demandée
//     * @param requestContext Le contexte de la requête
//     * @return L'entrée de journal créée
//     */
//    @Transactional
//    public ActivityLog logEmailChangeRequest(
//            User user,
//            Device device,
//            String newEmail,
//            RequestContext requestContext) {
//
//        String details = "Demande de changement d'email de " + user.getEmail() + " vers " + newEmail;
//
//        String metadataJson = null;
//        try {
//            Map<String, Object> metadata = new HashMap<>();
//            metadata.put("oldEmail", user.getEmail());
//            metadata.put("newEmail", newEmail);
//            metadata.put("requestTime", Instant.now().toString());
//            metadataJson = objectMapper.writeValueAsString(metadata);
//        } catch (JsonProcessingException e) {
//            log.warn("Impossible de sérialiser les métadonnées pour la demande de changement d'email: {}", e.getMessage());
//        }
//
//        return logUserAction(
//                user,
//                device,
//                oldActionLogType.EMAIL_CHANGE_REQUEST,
//                true,
//                details,
//                metadataJson,
//                requestContext
//        );
//    }
//
//    /**
//     * Enregistre l'annulation d'un changement d'adresse email
//     *
//     * @param user L'utilisateur concerné
//     * @param device L'appareil utilisé
//     * @param newEmail L'adresse email qui devait être changée
//     * @param requestContext Le contexte de la requête
//     * @return L'entrée de journal créée
//     */
//    @Transactional
//    public ActivityLog logEmailChangeCancellation(
//            User user,
//            Device device,
//            String newEmail,
//            RequestContext requestContext) {
//
//        String details = "Annulation du changement d'adresse email vers " + newEmail;
//
//        String metadataJson = null;
//        try {
//            Map<String, Object> metadata = new HashMap<>();
//            metadata.put("newEmail", newEmail);
//            metadata.put("cancellationTime", Instant.now().toString());
//            metadataJson = objectMapper.writeValueAsString(metadata);
//        } catch (JsonProcessingException e) {
//            log.warn("Impossible de sérialiser les métadonnées pour l'annulation du changement d'email: {}", e.getMessage());
//        }
//
//        return logUserAction(
//                user,
//                device,
//                oldActionLogType.EMAIL_CHANGE_CANCELLED,
//                true,
//                details,
//                metadataJson,
//                requestContext
//        );
//    }
//
//    /**
//     * Enregistre la vérification d'une nouvelle adresse email
//     *
//     * @param user L'utilisateur concerné
//     * @param device L'appareil utilisé
//     * @param newEmail L'adresse email vérifiée
//     * @param requestContext Le contexte de la requête
//     * @return L'entrée de journal créée
//     */
//    @Transactional
//    public ActivityLog logEmailChangeVerification(
//            User user,
//            Device device,
//            String newEmail,
//            RequestContext requestContext) {
//
//        String details = "Vérification de l'adresse email " + newEmail + " effectuée";
//
//        String metadataJson = null;
//        try {
//            Map<String, Object> metadata = new HashMap<>();
//            metadata.put("newEmail", newEmail);
//            metadata.put("verificationTime", Instant.now().toString());
//            metadataJson = objectMapper.writeValueAsString(metadata);
//        } catch (JsonProcessingException e) {
//            log.warn("Impossible de sérialiser les métadonnées pour la vérification d'email: {}", e.getMessage());
//        }
//
//        return logUserAction(
//                user,
//                device,
//                oldActionLogType.EMAIL_VERIFIED,
//                true,
//                details,
//                metadataJson,
//                requestContext
//        );
//    }
//
//    /**
//     * Enregistre la confirmation finale d'un changement d'adresse email
//     *
//     * @param user L'utilisateur concerné
//     * @param device L'appareil utilisé
//     * @param oldEmail L'ancienne adresse email
//     * @param newEmail La nouvelle adresse email
//     * @param requestContext Le contexte de la requête
//     * @return L'entrée de journal créée
//     */
//    @Transactional
//    public ActivityLog logEmailChangeComplete(
//            User user,
//            Device device,
//            String oldEmail,
//            String newEmail,
//            RequestContext requestContext) {
//
//        String details = "Changement d'email de " + oldEmail + " vers " + newEmail + " effectué";
//
//        String metadataJson = null;
//        try {
//            Map<String, Object> metadata = new HashMap<>();
//            metadata.put("oldEmail", oldEmail);
//            metadata.put("newEmail", newEmail);
//            metadata.put("completionTime", Instant.now().toString());
//            metadataJson = objectMapper.writeValueAsString(metadata);
//        } catch (JsonProcessingException e) {
//            log.warn("Impossible de sérialiser les métadonnées pour la confirmation du changement d'email: {}", e.getMessage());
//        }
//
//        return logUserAction(
//                user,
//                device,
//                oldActionLogType.EMAIL_CHANGE_COMPLETE,
//                true,
//                details,
//                metadataJson,
//                requestContext
//        );
//    }
//
//    /**
//     * Obtient l'historique des activités liées aux emails d'un utilisateur
//     *
//     * @param user L'utilisateur dont on veut l'historique
//     * @param days Nombre de jours d'historique à récupérer
//     * @return Une liste d'activités liées aux emails
//     */
//    @Transactional(readOnly = true)
//    public List<ActivityLog> getEmailActivityHistory(User user, int days) {
//        Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);
//        Instant endDate = Instant.now();
//
//        return activityLogRepository.findByUserAndActionTypeInAndTimestampBetween(
//                user,
//                List.of(
//                        oldActionLogType.EMAIL_CHANGE_REQUEST,
//                        oldActionLogType.EMAIL_CHANGE_CANCELLED,
//                        oldActionLogType.EMAIL_VERIFIED,
//                        oldActionLogType.EMAIL_CHANGE_COMPLETE
//                ),
//                startDate,
//                endDate,
//                null
//        ).getContent();
//    }
//
//    /**
//     * Calcule le nombre de jours depuis le dernier changement d'email
//     *
//     * @param user L'utilisateur pour lequel effectuer le calcul
//     * @return Le nombre de jours depuis le dernier changement ou -1 si aucun changement trouvé
//     */
//    @Transactional(readOnly = true)
//    public int daysSinceLastEmailChange(User user) {
//        ActivityLog lastEmailChange = activityLogRepository.findTopByUserAndActionTypeAndSuccessfulOrderByTimestampDesc(
//                user, oldActionLogType.EMAIL_CHANGE_COMPLETE.name(), true);
//
//        if (lastEmailChange == null) {
//            return -1;
//        }
//
//        Instant now = Instant.now();
//        Instant lastChange = lastEmailChange.getTimestamp();
//
//        return (int) ChronoUnit.DAYS.between(lastChange, now);
//    }
//
//    /**
//     * Vérifie si un utilisateur a atteint le nombre maximum de demandes de changement d'email
//     * dans un intervalle de temps donné
//     *
//     * @param user L'utilisateur à vérifier
//     * @param maxAttempts Le nombre maximum de tentatives autorisées
//     * @param timeframeMinutes L'intervalle de temps en minutes
//     * @return true si l'utilisateur a dépassé le seuil, false sinon
//     */
//    @Transactional(readOnly = true)
//    public boolean hasExceededEmailChangeRequestLimit(User user, int maxAttempts, int timeframeMinutes) {
//        Instant startTime = Instant.now().minus(timeframeMinutes, ChronoUnit.MINUTES);
//
//        long count = activityLogRepository.countByUserAndActionTypeAndTimestampBetween(
//                user,
//                oldActionLogType.EMAIL_CHANGE_REQUEST.name(),
//                startTime,
//                Instant.now()
//        );
//
//        return count >= maxAttempts;
//    }
//
//    /**
//     * Compte le nombre de changements d'email effectués par un utilisateur dans une période donnée
//     *
//     * @param user L'utilisateur concerné
//     * @param days Nombre de jours à considérer
//     * @return Le nombre de changements d'email
//     */
//    @Transactional(readOnly = true)
//    public long countEmailChangesInPeriod(User user, int days) {
//        Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);
//
//        return activityLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampBetween(
//                user,
//                oldActionLogType.EMAIL_CHANGE_COMPLETE.name(),
//                true,
//                startDate,
//                Instant.now()
//        );
//    }
//}