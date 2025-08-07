package be.steby.CoreProject.bll.domains.account.listeners;

import be.steby.CoreProject.bll.domains.account.events.AccountConfirmationEvent;
import be.steby.CoreProject.bll.domains.account.events.AccountDeactivationConfirmedEvent;
import be.steby.CoreProject.bll.domains.account.events.RequestAccountActivationEvent;
import be.steby.CoreProject.bll.domains.account.events.RequestAccountDeactivationEvent;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.bll.domains.account.services.AccountActivityLogService;
import be.steby.CoreProject.bll.common.utils.DeviceDetectionHelper;
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
 * - Demandes d'activation/désactivation
 * - Confirmation de désactivation
 */
@Component
@Order(20) // Ordre légèrement plus élevé pour s'assurer que les logs sont enregistrés après les autres traitements critiques
@RequiredArgsConstructor
@Slf4j
public class AccountActivityLogListener {

    private final ActivityLogService activityLogService;
    private final AccountActivityLogService accountActivityLogService;
    private final DeviceDetectionHelper deviceDetectionHelper;

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
     * Enregistre la confirmation d'une désactivation de compte
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

                    // La désactivation est généralement une auto-désactivation ou sans admin spécifique
                    // On peut passer null pour l'adminId ou l'ID de l'utilisateur lui-même
                    Long adminId = null; // Auto-désactivation
                    activityLogService.logAccountDeactivation(event.user(), adminId, event.requestContext());
                });
    }
}

/*
 * INTÉGRATION AVEC DeviceDetectionHelper ET AccountActivityLogService :
 *
 * Ce listener utilise maintenant :
 * - DeviceDetectionHelper : Pour la détection robuste des devices (pattern recommandé du projet)
 * - AccountActivityLogService : Service spécialisé pour tracer les activités de compte
 *
 * Cela offre une traçabilité complète :
 * - Les demandes (RequestAccountActivationEvent, RequestAccountDeactivationEvent)
 * - Les confirmations (AccountConfirmationEvent, AccountDeactivationConfirmedEvent)
 * - Détection automatique et sécurisée des devices
 * - Gestion d'erreurs intégrée dans DeviceDetectionHelper
 *
 * Le service spécialisé permet également des analyses avancées :
 * - Détection d'activité suspecte sur les comptes
 * - Statistiques sur les tentatives échouées
 * - Historique complet des activités de compte
 */