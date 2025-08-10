package be.steby.CoreProject.bll.domains.account.listeners;

import be.steby.CoreProject.bll.domains.account.events.AccountConfirmationEvent;
import be.steby.CoreProject.bll.domains.account.events.AccountDeactivationConfirmedEvent;
import be.steby.CoreProject.bll.domains.account.events.RequestAccountActivationEvent;
import be.steby.CoreProject.bll.domains.account.events.RequestAccountDeactivationEvent;
import be.steby.CoreProject.bll.domains.account.events.RequestAccountReactivationEvent;
import be.steby.CoreProject.bll.domains.account.events.AccountReactivationConfirmedEvent;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.bll.domains.account.services.AccountActivityLogService;
import be.steby.CoreProject.bll.common.utils.DeviceDetectionHelper;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener pour enregistrer les événements de compte dans les logs d'activité.
 * Utilise un executeur asynchrone dédié pour ne pas bloquer le thread principal.
 *
 * Ce listener capture tous les événements liés aux comptes utilisateur :
 * - Création de compte (SignupEvent)
 * - Confirmation de compte (AccountConfirmationEvent)
 * - Demandes d'activation/désactivation/réactivation
 * - Confirmations d'activation/désactivation/réactivation
 * - Gestion des tentatives échouées
 */
@Component
@Order(20) // Ordre légèrement plus élevé pour s'assurer que les logs sont enregistrés après les autres traitements critiques
@RequiredArgsConstructor
@Slf4j
public class AccountActivityLogListener {

    private final ActivityLogService activityLogService;
    private final AccountActivityLogService accountActivityLogService;
    private final DeviceDetectionHelper deviceDetectionHelper;

    // ================== ÉVÉNEMENTS PRINCIPAUX DE COMPTE ==================

    /**
     * Enregistre la création d'un nouveau compte utilisateur
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleSignupEvent(SignupEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> {
                    log.info("Logging account creation for user: {}", event.user().getEmail());
                    activityLogService.logAccountCreation(event.user(), device, event.requestContext());
                });
    }

    /**
     * Enregistre la confirmation d'un compte utilisateur (activation)
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAccountConfirmationEvent(AccountConfirmationEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> {
                    log.info("Logging account activation for user: {}", event.user().getEmail());
                    // Utilise logNewAccountActivation pour la première activation d'un nouveau compte
                    activityLogService.logNewAccountActivation(event.user(), device, event.requestContext());
                });
    }

    // ================== ÉVÉNEMENTS DE DEMANDES ==================

    /**
     * Enregistre une demande d'activation de compte
     *
     * Utilise maintenant le AccountActivityLogService spécialisé pour une meilleure traçabilité.
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestAccountActivationEvent(RequestAccountActivationEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> {
                    log.info("Logging account activation request for user: {}", event.user().getEmail());

                    // Utilise le service spécialisé pour les comptes
                    accountActivityLogService.logAccountActivationRequest(
                            event.user(),
                            device,
                            true, // Un token a été généré puisque l'événement est déclenché
                            event.requestContext()
                    );
                });
    }

    /**
     * Enregistre une demande de désactivation de compte
     *
     * Utilise maintenant le AccountActivityLogService spécialisé pour une meilleure traçabilité.
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestAccountDeactivationEvent(RequestAccountDeactivationEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> {
                    log.info("Logging account deactivation request for user: {} with reason: {}",
                            event.user().getEmail(), event.deactivationReason());

                    // Utilise le service spécialisé pour les comptes
                    accountActivityLogService.logAccountDeactivationRequest(
                            event.user(),
                            device,
                            event.deactivationReason(),
                            event.reasonDetails(),
                            true, // Un token a été généré puisque l'événement est déclenché
                            event.requestContext()
                    );
                });
    }

    /**
     * Enregistre une demande de réactivation de compte
     *
     * Utilise le AccountActivityLogService spécialisé pour une meilleure traçabilité.
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestAccountReactivationEvent(RequestAccountReactivationEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> {
                    log.info("Logging account reactivation request for user: {}", event.user().getEmail());

                    // Utilise le service spécialisé pour les comptes
                    accountActivityLogService.logAccountReactivationRequest(
                            event.user(),
                            device,
                            true, // Un token a été généré puisque l'événement est déclenché
                            event.requestContext()
                    );
                });
    }

    // ================== ÉVÉNEMENTS DE CONFIRMATIONS ==================

    /**
     * Enregistre la confirmation d'une désactivation de compte
     *
     * CORRECTION: Utilise maintenant le service spécialisé au lieu du service générique
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAccountDeactivationConfirmedEvent(AccountDeactivationConfirmedEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> {
                    log.info("Logging account deactivation confirmation for user: {} with reason: {}",
                            event.user().getEmail(), event.deactivationReason());

                    // CORRECTION: Utilise le service spécialisé pour la désactivation confirmée
                    // au lieu de activityLogService.logAccountDeactivation
                    accountActivityLogService.logAccountDeactivationConfirmed(
                            event.user(),
                            device,
                            event.deactivationReason(),
                            event.reasonDetails(),
                            event.requestContext()
                    );
                });
    }

    /**
     * Enregistre la confirmation d'une réactivation de compte
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAccountReactivationConfirmedEvent(AccountReactivationConfirmedEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> {
                    log.info("Logging account reactivation confirmation for user: {}", event.user().getEmail());

                    // Utilise le service spécialisé pour une réactivation confirmée
                    accountActivityLogService.logAccountReactivationConfirmed(
                            event.user(),
                            device,
                            event.requestContext()
                    );
                });
    }

    // ================== MÉTHODES UTILITAIRES POUR LES ÉCHECS ==================

    /**
     * Méthode utilitaire pour enregistrer une tentative d'activation échouée
     * À utiliser dans les controllers/services lors de la validation de tokens
     *
     * @param user L'utilisateur concerné
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     */
    public void logFailedActivationAttempt(User user, String failureReason, String tokenUsed, RequestContext requestContext) {
        deviceDetectionHelper.executeWithDeviceDetection(
                user,
                requestContext,
                device -> {
                    log.warn("Logging failed activation attempt for user: {} - Reason: {}", user.getEmail(), failureReason);

                    accountActivityLogService.logAccountActivationAttemptFailed(
                            user,
                            device,
                            failureReason,
                            tokenUsed,
                            requestContext
                    );
                });
    }

    /**
     * Méthode utilitaire pour enregistrer une tentative de désactivation échouée
     * À utiliser dans les controllers/services lors de la validation de tokens
     *
     * @param user L'utilisateur concerné
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     */
    public void logFailedDeactivationAttempt(User user, String failureReason, String tokenUsed, RequestContext requestContext) {
        deviceDetectionHelper.executeWithDeviceDetection(
                user,
                requestContext,
                device -> {
                    log.warn("Logging failed deactivation attempt for user: {} - Reason: {}", user.getEmail(), failureReason);

                    accountActivityLogService.logAccountDeactivationAttemptFailed(
                            user,
                            device,
                            failureReason,
                            tokenUsed,
                            requestContext
                    );
                });
    }

    /**
     * Méthode utilitaire pour enregistrer une tentative de réactivation échouée
     * À utiliser dans les controllers/services lors de la validation de tokens
     *
     * @param user L'utilisateur concerné
     * @param failureReason La raison de l'échec
     * @param tokenUsed Le token utilisé (partiel pour sécurité)
     * @param requestContext Le contexte de la requête
     */
    public void logFailedReactivationAttempt(User user, String failureReason, String tokenUsed, RequestContext requestContext) {
        deviceDetectionHelper.executeWithDeviceDetection(
                user,
                requestContext,
                device -> {
                    log.warn("Logging failed reactivation attempt for user: {} - Reason: {}", user.getEmail(), failureReason);

                    accountActivityLogService.logAccountReactivationAttemptFailed(
                            user,
                            device,
                            failureReason,
                            tokenUsed,
                            requestContext
                    );
                });
    }

    /**
     * Méthode utilitaire pour enregistrer la révocation d'un token
     * À utiliser dans les services lors de révocations de tokens
     *
     * @param user L'utilisateur concerné
     * @param tokenType Le type de token révoqué
     * @param revocationReason La raison de révocation
     * @param requestContext Le contexte de la requête
     */
    public void logTokenRevocation(User user, String tokenType, String revocationReason, RequestContext requestContext) {
        deviceDetectionHelper.executeWithDeviceDetection(
                user,
                requestContext,
                device -> {
                    log.info("Logging token revocation for user: {} - Token type: {} - Reason: {}",
                            user.getEmail(), tokenType, revocationReason);

                    accountActivityLogService.logAccountTokenRevocation(
                            user,
                            device,
                            tokenType,
                            revocationReason,
                            requestContext
                    );
                });
    }
}

/*
 * INTÉGRATION COMPLÈTE AVEC DeviceDetectionHelper ET AccountActivityLogService :
 *
 * Ce listener utilise maintenant :
 * - DeviceDetectionHelper : Pour la détection robuste des devices (pattern recommandé du projet)
 * - AccountActivityLogService : Service spécialisé pour tracer les activités de compte
 *
 * COUVERTURE COMPLÈTE DES ÉVÉNEMENTS :
 * ✅ Création de compte (SignupEvent)
 * ✅ Confirmation de compte (AccountConfirmationEvent)
 * ✅ Demandes d'activation (RequestAccountActivationEvent)
 * ✅ Demandes de désactivation (RequestAccountDeactivationEvent)
 * ✅ Demandes de réactivation (RequestAccountReactivationEvent)
 * ✅ Confirmations de désactivation (AccountDeactivationConfirmedEvent)
 * ✅ Confirmations de réactivation (AccountReactivationConfirmedEvent)
 * ✅ Gestion des tentatives échouées (méthodes utilitaires)
 * ✅ Révocation de tokens (méthode utilitaire)
 *
 * TRAÇABILITÉ COMPLÈTE :
 * - Les demandes (Request events)
 * - Les confirmations (Confirmed events)
 * - Les échecs (Failed attempt methods)
 * - Détection automatique et sécurisée des devices
 * - Gestion d'erreurs intégrée dans DeviceDetectionHelper
 *
 * ANALYSES AVANCÉES POSSIBLES :
 * - Détection d'activité suspecte sur les comptes
 * - Statistiques sur les tentatives échouées
 * - Historique complet des activités de compte
 * - Audit de sécurité et conformité
 */